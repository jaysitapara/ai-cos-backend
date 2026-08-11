package com.app.service;

import com.app.entity.ProjectCreationSessionEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.ProjectQuestionEntity;
import com.app.entity.UserEntity;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.UnauthorizedException;
import com.app.repository.ProjectCreationSessionRepository;
import com.app.repository.ProjectQuestionRepository;
import com.app.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementQuestionEngine {

    private final TechnologyCatalogService catalogService;
    private final AIService aiService;
    private final ProjectCreationSessionRepository sessionRepository;
    private final ProjectQuestionRepository questionRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProjectCreationSessionEntity startSession(UserEntity user, String initialPrompt) {
        log.info("Starting requirement analysis session for user {} with prompt: {}", user.getEmail(), initialPrompt);

        // 1. Create creation session entity
        ProjectCreationSessionEntity session = ProjectCreationSessionEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .initialPrompt(initialPrompt)
                .status("IN_PROGRESS")
                .currentStep(1)
                .totalSteps(5)
                .build();
        session = sessionRepository.save(session);

        // 2. Fetch catalog questions (deterministic — 0 AI tokens)
        List<TechnologyCatalogService.TechQuestionDefinition> catalogQuestions = catalogService.getStandardCatalogQuestions();

        // 3. Single AI recommendation call per session start
        String aiRecommendationText = "";
        try {
            String systemInstruction = "You are a senior software architect. Analyze the project prompt and output a brief JSON map of recommended tech keys: frontend, backend, database, authentication, styling.";
            aiRecommendationText = aiService.analyzePrompt(systemInstruction, initialPrompt, user, null, "ARCHITECT");
        } catch (Exception e) {
            log.warn("AI recommendation call failed, using default catalog recommendations: {}", e.getMessage());
        }

        // 4. Build and save question entities
        List<ProjectQuestionEntity> questionEntities = new ArrayList<>();
        int index = 1;
        for (TechnologyCatalogService.TechQuestionDefinition def : catalogQuestions) {
            String optionsJson = "[]";
            try {
                optionsJson = objectMapper.writeValueAsString(def.getOptions());
            } catch (Exception ignored) {}

            String recommendedOption = def.getDefaultOptionId();
            if (aiRecommendationText.toLowerCase().contains("react") && def.getKey().equals("frontend")) recommendedOption = "react";
            if (aiRecommendationText.toLowerCase().contains("fastapi") && def.getKey().equals("backend")) recommendedOption = "fastapi";
            if (aiRecommendationText.toLowerCase().contains("mongodb") && def.getKey().equals("database")) recommendedOption = "mongodb";

            ProjectQuestionEntity q = ProjectQuestionEntity.builder()
                    .session(session)
                    .questionKey(def.getKey())
                    .title(def.getTitle())
                    .description(def.getDescription())
                    .category(def.getCategory())
                    .optionsJson(optionsJson)
                    .selectedOption(recommendedOption)
                    .customValue(null)
                    .isRecommended(true)
                    .recommendationReason("Recommended based on initial project requirements analysis")
                    .orderIndex(index++)
                    .status("PENDING")
                    .build();
            questionEntities.add(q);
        }

        questionRepository.saveAll(questionEntities);
        if (session.getQuestions() == null) {
            session.setQuestions(new ArrayList<>());
        } else {
            session.getQuestions().clear();
        }
        session.getQuestions().addAll(questionEntities);
        session.setTotalSteps(questionEntities.size());
        return sessionRepository.save(session);
    }

    /**
     * Loads a session with questions eagerly via JOIN FETCH.
     * Safe to call from both read and write transactions.
     */
    @Transactional
    public ProjectCreationSessionEntity getSession(UUID sessionPublicId, UserEntity user) {
        ProjectCreationSessionEntity session = sessionRepository.findByPublicIdWithQuestions(sessionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Creation session not found: " + sessionPublicId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Session does not belong to user");
        }
        return session;
    }

    @Transactional
    public ProjectCreationSessionEntity saveAnswers(UUID sessionPublicId, UserEntity user, Map<String, String> answers, Map<String, String> customValues) {
        // Load session via dedicated write-transaction-safe method (no readOnly nesting)
        ProjectCreationSessionEntity session = sessionRepository.findByPublicIdWithQuestions(sessionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Creation session not found: " + sessionPublicId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Session does not belong to user");
        }

        List<ProjectQuestionEntity> questions = session.getQuestions();
        if (questions == null || questions.isEmpty()) {
            questions = questionRepository.findBySessionIdOrderByOrderIndexAsc(session.getId());
        }

        for (ProjectQuestionEntity q : questions) {
            if (answers != null && answers.containsKey(q.getQuestionKey())) {
                q.setSelectedOption(answers.get(q.getQuestionKey()));
                q.setStatus("ANSWERED");
            }
            if (customValues != null && customValues.containsKey(q.getQuestionKey())) {
                q.setCustomValue(customValues.get(q.getQuestionKey()));
            }
        }
        questionRepository.saveAll(questions);

        long answeredCount = questions.stream().filter(q -> "ANSWERED".equals(q.getStatus())).count();
        session.setCurrentStep(Math.min((int) answeredCount + 1, session.getTotalSteps()));
        if (answeredCount >= session.getTotalSteps()) {
            session.setStatus("REVIEW_READY");
        }

        // Build configuration JSON summary
        Map<String, String> configMap = new LinkedHashMap<>();
        for (ProjectQuestionEntity q : questions) {
            String val = "other".equalsIgnoreCase(q.getSelectedOption()) && q.getCustomValue() != null && !q.getCustomValue().isBlank()
                    ? q.getCustomValue()
                    : q.getSelectedOption();
            configMap.put(q.getQuestionKey(), val);
        }
        try {
            session.setConfigurationJson(objectMapper.writeValueAsString(configMap));
        } catch (Exception ignored) {}

        return sessionRepository.save(session);
    }

    @Transactional
    public ProjectEntity finalizeProjectConfiguration(UUID sessionPublicId, UserEntity user) {
        ProjectCreationSessionEntity session = sessionRepository.findByPublicIdWithQuestions(sessionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Creation session not found: " + sessionPublicId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Session does not belong to user");
        }

        List<ProjectQuestionEntity> questions = session.getQuestions();
        if (questions == null || questions.isEmpty()) {
            questions = questionRepository.findBySessionIdOrderByOrderIndexAsc(session.getId());
        }

        Map<String, String> stackMap = new LinkedHashMap<>();
        for (ProjectQuestionEntity q : questions) {
            String val = "other".equalsIgnoreCase(q.getSelectedOption()) && q.getCustomValue() != null && !q.getCustomValue().isBlank()
                    ? q.getCustomValue()
                    : q.getSelectedOption();
            stackMap.put(q.getQuestionKey(), val);
        }

        String stackJson = "{}";
        try {
            stackJson = objectMapper.writeValueAsString(stackMap);
        } catch (Exception ignored) {}

        String projectName = deriveProjectName(session.getInitialPrompt());

        ProjectEntity project = ProjectEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .name(projectName)
                .description(session.getInitialPrompt())
                .status("QUEUED")
                .progress(0)
                .runtimeStatus("STOPPED")
                .activeTechStackJson(stackJson)
                .build();
        project = projectRepository.save(project);

        session.setProject(project);
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        log.info("Project configuration finalized cleanly: Project ID {} for User {}", project.getPublicId(), user.getEmail());
        return project;
    }

    private String deriveProjectName(String prompt) {
        if (prompt == null || prompt.isBlank()) return "Generated Application";
        String clean = prompt.trim();
        if (clean.length() > 40) {
            clean = clean.substring(0, 40) + "...";
        }
        return clean.replaceAll("[^a-zA-Z0-9\\s\\-_.]", "");
    }
}
