package com.app.entity;

import com.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "agent_workspace_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkspacePlanEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AgentWorkspaceSessionEntity session;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "business_requirements", columnDefinition = "TEXT")
    private String businessRequirements;

    @Column(name = "functional_requirements", columnDefinition = "TEXT")
    private String functionalRequirements;

    @Column(name = "non_functional_requirements", columnDefinition = "TEXT")
    private String nonFunctionalRequirements;

    @Column(name = "user_stories_json", columnDefinition = "TEXT")
    private String userStoriesJson;

    @Column(name = "features_modules_json", columnDefinition = "TEXT")
    private String featuresModulesJson;

    @Column(name = "dependencies_json", columnDefinition = "TEXT")
    private String dependenciesJson;

    @Column(name = "risks_json", columnDefinition = "TEXT")
    private String risksJson;

    @Column(name = "milestones_json", columnDefinition = "TEXT")
    private String milestonesJson;

    @Column(name = "system_architecture", columnDefinition = "TEXT")
    private String systemArchitecture;

    @Column(name = "folder_structure", columnDefinition = "TEXT")
    private String folderStructure;

    @Column(name = "database_design", columnDefinition = "TEXT")
    private String databaseDesign;

    @Column(name = "api_design", columnDefinition = "TEXT")
    private String apiDesign;

    @Column(name = "er_diagram_mermaid", columnDefinition = "TEXT")
    private String erDiagramMermaid;

    @Column(name = "tech_stack_json", columnDefinition = "TEXT")
    private String techStackJson;

    @Column(name = "auth_flow", columnDefinition = "TEXT")
    private String authFlow;

    @Column(name = "deployment_architecture", columnDefinition = "TEXT")
    private String deploymentArchitecture;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus;

    @Column(name = "approval_feedback", columnDefinition = "TEXT")
    private String approvalFeedback;
}
