package com.app.service;

import com.app.model.backendgen.BackendPlan;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BackendGeneratorService {

    private final Map<String, BackendPlan> planStore = new ConcurrentHashMap<>();

    public BackendPlan generateBackendPlan(String blueprintId) {
        String planId = "bep-" + UUID.randomUUID();

        List<String> modules = List.of(
            "com.app.controller.v1",
            "com.app.service",
            "com.app.repository",
            "com.app.model",
            "com.app.config",
            "com.app.enums"
        );

        Map<String, Object> testing = Map.of(
            "unitTestFramework", "JUnit 5 + Mockito",
            "integrationTestFramework", "Spring Boot Test + MockMvc",
            "targetCoveragePercent", 85,
            "testCountGoal", 40
        );

        Map<String, String> docs = Map.of(
            "README.md", "# Application Setup Guide\n\nRun `./gradlew bootRun` to launch backend.",
            "API.md", "# REST API Specification\n\nAll endpoints prefixed with `/api/v1/`.",
            "DEPLOYMENT.md", "# Production Deployment Notes\n\nRequires Java 17 runtime and PostgreSQL 15."
        );

        BackendPlan plan = BackendPlan.builder()
            .planId(planId)
            .blueprintId(blueprintId)
            .featureModules(modules)
            .testingStrategy(testing)
            .documentationArtifacts(docs)
            .createdAt(Instant.now())
            .build();

        planStore.put(planId, plan);
        return plan;
    }

    public Optional<BackendPlan> getPlan(String planId) {
        return Optional.ofNullable(planStore.get(planId));
    }
}
