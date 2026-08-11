package com.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_performance_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentPerformanceSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_analytics_id", nullable = false)
    private ContentAnalyticsEntity contentAnalytics;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "impressions")
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "reach")
    @Builder.Default
    private Long reach = 0L;

    @Column(name = "likes")
    @Builder.Default
    private Long likes = 0L;

    @Column(name = "comments")
    @Builder.Default
    private Long comments = 0L;

    @Column(name = "shares")
    @Builder.Default
    private Long shares = 0L;

    @Column(name = "saves")
    @Builder.Default
    private Long saves = 0L;

    @Column(name = "clicks")
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions")
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "engagement_rate")
    @Builder.Default
    private Double engagementRate = 0.0;

    @Column(name = "snapshot_at")
    @Builder.Default
    private OffsetDateTime snapshotAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (snapshotAt == null) snapshotAt = OffsetDateTime.now();
    }
}
