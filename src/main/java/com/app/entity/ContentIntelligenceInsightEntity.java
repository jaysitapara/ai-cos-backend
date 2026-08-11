package com.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_intelligence_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentIntelligenceInsightEntity {

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

    @Column(name = "type", nullable = false)
    private String type; // PREFERENCE, STYLE, CONTENT_TYPE, TOPIC, TONE, AVERSION, LENGTH, PERFORMANCE

    @Column(name = "insight_key", nullable = false)
    private String insightKey;

    @Column(name = "insight_value", nullable = false, columnDefinition = "TEXT")
    private String insightValue;

    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private String confidence = "LOW"; // LOW, MEDIUM, HIGH

    @Column(name = "confidence_score")
    @Builder.Default
    private Double confidenceScore = 0.5;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(name = "evidence_count")
    @Builder.Default
    private Integer evidenceCount = 1;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, WEAKENED, ARCHIVED

    @Column(name = "scope", nullable = false)
    @Builder.Default
    private String scope = "BRAND"; // BRAND, USER, CONTENT_TYPE

    @Column(name = "content_type_scope")
    private String contentTypeScope;

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
