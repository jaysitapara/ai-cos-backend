package com.app.service;

import com.app.model.specification.SpecificationBundle;
import com.app.model.specification.UserStoryItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SpecificationGenerator {

    private final Map<String, SpecificationBundle> specStore = new ConcurrentHashMap<>();

    public SpecificationBundle generateSpecifications(String analysisId) {
        String specId = "spec-" + UUID.randomUUID();

        String brd = """
            # Business Requirements Document (BRD)
            ## Executive Summary
            Automated platform goal execution and code generation baseline.
            ## Business Goals
            - Reduce development cycle time by 80%.
            - Enforce production security and code architecture quality.
            """;

        String prd = """
            # Product Requirements Document (PRD)
            ## Product Vision
            Provide seamless AI Workspace enabling users to prompt, plan, and autonomously execute complex software projects.
            ## Functional Requirements
            - User authentication with JWT & OAuth
            - AI Workspace prompt composer with multimodal attachments
            - Automatic requirement analysis, research, and architecture generation
            """;

        UserStoryItem story1 = UserStoryItem.builder()
            .storyId("US-101")
            .epic("Core Platform Foundation")
            .persona("System User")
            .title("Submit Goal Prompt & Attachments")
            .userStory("As a user, I want to submit my software goal so that the system analyzes requirements.")
            .acceptanceCriteria(List.of(
                "Given a prompt and attachment file, when submitted, then return requirement analysis ID.",
                "Given invalid input, when submitted, then display clean error toast."
            ))
            .priority("HIGH")
            .dependencies(List.of())
            .estimatedComplexity("2 SP")
            .status("READY")
            .build();

        SpecificationBundle bundle = SpecificationBundle.builder()
            .specId(specId)
            .analysisId(analysisId)
            .brdContent(brd)
            .prdContent(prd)
            .userStories(List.of(story1))
            .functionalSpecs(Map.of("apiPrefix", "/api/v1/", "authMethod", "JWT Header"))
            .createdAt(Instant.now())
            .build();

        specStore.put(specId, bundle);
        return bundle;
    }

    public Optional<SpecificationBundle> getSpecification(String specId) {
        return Optional.ofNullable(specStore.get(specId));
    }
}
