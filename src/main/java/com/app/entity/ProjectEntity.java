package com.app.entity;

import com.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private WorkspaceEntity workspace;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "progress", nullable = false)
    private Integer progress;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "runtime_status", length = 50)
    private String runtimeStatus;

    @Column(name = "runtime_port")
    private Integer runtimePort;

    @Column(name = "container_id", length = 128)
    private String containerId;

    @Column(name = "execution_mode", length = 32)
    @Builder.Default
    private String executionMode = "container";

    @Column(name = "active_tech_stack_json", columnDefinition = "TEXT")
    private String activeTechStackJson;

    @Column(name = "preview_url", length = 255)
    private String previewUrl;

    @Column(name = "last_run_at")
    private OffsetDateTime lastRunAt;

    @PrePersist
    protected void onCreateDefaults() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (status == null) {
            status = "QUEUED";
        }
        if (progress == null) {
            progress = 0;
        }
        if (runtimeStatus == null) {
            runtimeStatus = "STOPPED";
        }
    }
}
