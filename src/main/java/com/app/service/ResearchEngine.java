package com.app.service;

import com.app.enums.RiskCategory;
import com.app.model.research.ResearchResult;
import com.app.model.research.RiskItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResearchEngine {

    private final Map<String, ResearchResult> researchStore = new ConcurrentHashMap<>();

    public ResearchResult executeResearch(String analysisId) {
        String researchId = "res-" + UUID.randomUUID();

        RiskItem r1 = RiskItem.builder()
            .riskId("risk-1")
            .title("High Concurrent Request Load")
            .category(RiskCategory.PERFORMANCE)
            .severity("MEDIUM")
            .description("Surge in traffic may impact database connection pools")
            .mitigation("Implement connection pool tuning and Redis cache layer")
            .build();

        RiskItem r2 = RiskItem.builder()
            .riskId("risk-2")
            .title("Unauthenticated API Exposure")
            .category(RiskCategory.SECURITY)
            .severity("HIGH")
            .description("Public REST endpoints exposing internal state")
            .mitigation("Enforce JWT authorization filter on /api/v1/ routes")
            .build();

        List<String> recommendations = List.of(
            "Use Clean Architecture with feature-first packages",
            "Enforce strict OpenAPI/Swagger API contracts",
            "Incorporate automated unit test suite with 80%+ coverage"
        );

        List<Map<String, Object>> milestones = List.of(
            Map.of("milestone", "M1 - Core Specs & Architecture", "durationDays", 2),
            Map.of("milestone", "M2 - Backend APIs & Security", "durationDays", 3),
            Map.of("milestone", "M3 - UI Implementation & Testing", "durationDays", 3)
        );

        ResearchResult result = ResearchResult.builder()
            .researchId(researchId)
            .analysisId(analysisId)
            .domainClassification("ENTERPRISE_WEB_PLATFORM")
            .technicalRecommendations(recommendations)
            .riskAnalysis(List.of(r1, r2))
            .milestones(milestones)
            .estimatedEffortDays(8)
            .createdAt(Instant.now())
            .build();

        researchStore.put(researchId, result);
        return result;
    }

    public Optional<ResearchResult> getResult(String researchId) {
        return Optional.ofNullable(researchStore.get(researchId));
    }
}
