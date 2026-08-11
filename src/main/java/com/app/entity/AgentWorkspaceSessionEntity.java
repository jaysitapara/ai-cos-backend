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
@Table(name = "agent_workspace_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkspaceSessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "goal_prompt", nullable = false, columnDefinition = "TEXT")
    private String goalPrompt;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(name = "complexity", length = 50)
    private String complexity;

    @Column(name = "estimated_scope", length = 100)
    private String estimatedScope;

    @Column(name = "uploaded_files_json", columnDefinition = "TEXT")
    private String uploadedFilesJson;

    @Column(name = "assumptions_json", columnDefinition = "TEXT")
    private String assumptionsJson;

    @Column(name = "missing_info_json", columnDefinition = "TEXT")
    private String missingInfoJson;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 50)
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.AUTO;
}
