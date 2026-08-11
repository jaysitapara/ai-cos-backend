package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "user_comment_id")
    private Long userCommentId;

    @Column(name = "changed_files_json", columnDefinition = "TEXT")
    private String changedFilesJson;

    @Column(name = "generation_metadata_json", columnDefinition = "TEXT")
    private String generationMetadataJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "COMPLETED";
        }
    }
}
