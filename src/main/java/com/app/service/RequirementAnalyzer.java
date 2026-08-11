package com.app.service;

import com.app.enums.RequirementCategory;
import com.app.model.requirement.RequirementAnalysisResult;
import com.app.model.requirement.RequirementItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequirementAnalyzer {

    private final Map<String, RequirementAnalysisResult> analysisStore = new ConcurrentHashMap<>();

    public RequirementAnalysisResult analyzeGoal(String goalPrompt, List<String> attachmentIds) {
        String analysisId = "req-" + UUID.randomUUID();

        RequirementItem req1 = RequirementItem.builder()
            .requirementId("req-item-1")
            .analysisId(analysisId)
            .category(RequirementCategory.FUNCTIONAL)
            .title("Core Goal Processing Pipeline")
            .description("Process user prompt input and generate structured software artifacts")
            .priority("HIGH")
            .source("PROMPT")
            .confidenceScore(0.95)
            .dependencies(List.of())
            .assumptions(List.of("User input prompt is non-empty"))
            .status("IDENTIFIED")
            .build();

        RequirementItem req2 = RequirementItem.builder()
            .requirementId("req-item-2")
            .analysisId(analysisId)
            .category(RequirementCategory.NON_FUNCTIONAL)
            .title("Low-Latency API Response & Security")
            .description("API requests must complete within 2000ms with strict rate limiting and JWT auth")
            .priority("HIGH")
            .source("INFERRED")
            .confidenceScore(0.90)
            .dependencies(List.of("req-item-1"))
            .assumptions(List.of("Standard JWT header present"))
            .status("IDENTIFIED")
            .build();

        List<RequirementItem> requirements = List.of(req1, req2);
        List<String> assumptions = List.of(
            "System deploys to standard Spring Boot / React container runtime",
            "PostgreSQL database handles relational persistence"
        );
        List<String> ambiguities = List.of();

        RequirementAnalysisResult result = RequirementAnalysisResult.builder()
            .analysisId(analysisId)
            .goalPrompt(goalPrompt)
            .projectType("FULL_STACK")
            .estimatedComplexity("MEDIUM")
            .requirements(requirements)
            .assumptions(assumptions)
            .detectedAmbiguities(ambiguities)
            .createdAt(Instant.now())
            .build();

        analysisStore.put(analysisId, result);
        return result;
    }

    public Optional<RequirementAnalysisResult> getAnalysis(String analysisId) {
        return Optional.ofNullable(analysisStore.get(analysisId));
    }

    public RequirementAnalysisResult updateAssumptions(String analysisId, List<String> assumptions) {
        RequirementAnalysisResult result = analysisStore.get(analysisId);
        if (result == null) {
            throw new IllegalArgumentException("Analysis not found: " + analysisId);
        }
        result.setAssumptions(assumptions);
        return result;
    }
}
