package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.entity.ProjectCreationSessionEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.UserEntity;
import com.app.exception.UnauthorizedException;
import com.app.repository.UserRepository;
import com.app.service.RequirementQuestionEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/projects/requirements")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = "V1 Requirement Analysis", description = "Dynamic Tech Stack Requirement Question Flow APIs")
public class V1ProjectRequirementController {

    private final RequirementQuestionEngine questionEngine;
    private final UserRepository userRepository;

    @Data
    public static class StartRequest {
        private String initialPrompt;
    }

    @Data
    public static class AnswersRequest {
        private Map<String, String> answers;
        private Map<String, String> customValues;
    }

    @PostMapping("/start")
    @Transactional
    @Operation(summary = "Analyze initial project prompt & start requirement question session")
    public ResponseEntity<Map<String, Object>> startRequirementAnalysis(@RequestBody StartRequest request, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectCreationSessionEntity session = questionEngine.startSession(user, request.getInitialPrompt());

        return ResponseEntity.ok(mapSession(session));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get requirement creation session & question state")
    public ResponseEntity<Map<String, Object>> getRequirementSession(@PathVariable UUID sessionId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectCreationSessionEntity session = questionEngine.getSession(sessionId, user);

        return ResponseEntity.ok(mapSession(session));
    }

    @PostMapping("/{sessionId}/answers")
    @Transactional
    @Operation(summary = "Save requirement question answers")
    public ResponseEntity<Map<String, Object>> saveRequirementAnswers(@PathVariable UUID sessionId,
                                                                       @RequestBody AnswersRequest request,
                                                                       Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        Map<String, String> answers = request != null ? request.getAnswers() : Map.of();
        Map<String, String> customs = request != null ? request.getCustomValues() : Map.of();

        ProjectCreationSessionEntity session = questionEngine.saveAnswers(sessionId, user, answers, customs);

        return ResponseEntity.ok(mapSession(session));
    }

    @PostMapping("/{sessionId}/complete")
    @Transactional
    @Operation(summary = "Finalize requirement configuration & create project")
    public ResponseEntity<Map<String, Object>> finalizeConfiguration(@PathVariable UUID sessionId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = questionEngine.finalizeProjectConfiguration(sessionId, user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("projectId", project.getPublicId().toString());
        res.put("name", project.getName());
        res.put("status", project.getStatus());
        res.put("activeTechStackJson", project.getActiveTechStackJson());
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> mapSession(ProjectCreationSessionEntity s) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("sessionId", s.getPublicId().toString());
        res.put("initialPrompt", s.getInitialPrompt());
        res.put("status", s.getStatus());
        res.put("currentStep", s.getCurrentStep());
        res.put("totalSteps", s.getTotalSteps());
        res.put("configurationJson", s.getConfigurationJson() != null ? s.getConfigurationJson() : "{}");
        if (s.getProject() != null && Hibernate.isInitialized(s.getProject())) {
            try {
                res.put("projectId", s.getProject().getPublicId().toString());
            } catch (Exception ignored) {}
        }

        List<Map<String, Object>> qList = (s.getQuestions() != null ? s.getQuestions() : List.<com.app.entity.ProjectQuestionEntity>of())
                .stream().map(q -> {
            Map<String, Object> qm = new LinkedHashMap<>();
            qm.put("id", q.getId());
            qm.put("key", q.getQuestionKey());
            qm.put("title", q.getTitle());
            qm.put("description", q.getDescription());
            qm.put("category", q.getCategory());
            qm.put("optionsJson", q.getOptionsJson());
            qm.put("selectedOption", q.getSelectedOption());
            qm.put("customValue", q.getCustomValue());
            qm.put("isRecommended", q.getIsRecommended());
            qm.put("recommendationReason", q.getRecommendationReason());
            qm.put("orderIndex", q.getOrderIndex());
            qm.put("status", q.getStatus());
            return qm;
        }).collect(Collectors.toList());

        res.put("questions", qList);
        return res;
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User authentication required");
        }
        String name = authentication.getName();
        try {
            UUID publicId = UUID.fromString(name);
            return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                    .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(name)
                            .orElseThrow(() -> new UnauthorizedException("Authenticated user not found")));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        }
    }
}
