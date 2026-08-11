package com.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private GeneratedContentEntity content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_version_id")
    private ContentVersionEntity contentVersion;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "topic")
    private String topic;

    @Column(name = "platform")
    @Builder.Default
    private String platform = "OTHER";

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "edit_count")
    @Builder.Default
    private Integer editCount = 0;

    @Column(name = "version_count")
    @Builder.Default
    private Integer versionCount = 1;

    @Column(name = "performance_score")
    @Builder.Default
    private Double performanceScore = 0.0;

    @Column(name = "generated_at")
    @Builder.Default
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
