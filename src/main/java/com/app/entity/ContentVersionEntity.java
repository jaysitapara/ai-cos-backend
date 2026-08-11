package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private GeneratedContentEntity content;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "change_source", nullable = false, length = 50)
    private String changeSource;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (changeSource == null) changeSource = "AI_INITIAL";
    }
}
