package com.app.service;

import com.app.model.crossproject.CrossProjectRecommendation;
import com.app.model.crossproject.ProjectRelationshipGraph;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CrossProjectIntelligenceService {

    public List<CrossProjectRecommendation> getRecommendations(String targetProjectId) {
        CrossProjectRecommendation r1 = CrossProjectRecommendation.builder()
            .recommendationId("rec-1")
            .sourceProjectId("proj-auth-service")
            .targetProjectId(targetProjectId != null ? targetProjectId : "proj-current")
            .type("REUSABLE_MODULE")
            .title("Reuse JWT Token Rotation Filter")
            .description("Found high similarity with Auth Service security layer. Reuse rate-limiting and refresh token rotation filter.")
            .confidenceScore(0.96)
            .rationale("98% code pattern match across authentication modules")
            .build();

        CrossProjectRecommendation r2 = CrossProjectRecommendation.builder()
            .recommendationId("rec-2")
            .sourceProjectId("proj-dashboard")
            .targetProjectId(targetProjectId != null ? targetProjectId : "proj-current")
            .type("ARCHITECTURE_PATTERN")
            .title("Reuse Micro-Frontend Component Composition")
            .description("Leverage existing React dynamic service provider wrapper pattern.")
            .confidenceScore(0.92)
            .rationale("Identical UI state composition requirements")
            .build();

        return List.of(r1, r2);
    }

    public ProjectRelationshipGraph getRelationshipGraph(String projectId) {
        return ProjectRelationshipGraph.builder()
            .projectId(projectId != null ? projectId : "proj-current")
            .relatedProjectIds(List.of("proj-auth-service", "proj-dashboard", "proj-analytics"))
            .sharedComponentsCount(14)
            .sharedApisCount(8)
            .sharedTechnologies(List.of("Java 17", "Spring Boot", "React", "TypeScript", "Tailwind CSS"))
            .build();
    }
}
