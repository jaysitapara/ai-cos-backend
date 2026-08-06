package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImplementationPlanResponse {
    private UUID publicId;
    private String executiveSummary;
    private String businessRequirements;
    private String functionalRequirements;
    private String nonFunctionalRequirements;
    private List<String> userStories;
    private List<String> featuresModules;
    private List<String> dependencies;
    private List<String> risks;
    private List<String> milestones;
    private String systemArchitecture;
    private String folderStructure;
    private String databaseDesign;
    private String apiDesign;
    private String erDiagramMermaid;
    private List<String> techStack;
    private String authFlow;
    private String deploymentArchitecture;
    private String approvalStatus;
    private String approvalFeedback;
}
