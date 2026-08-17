package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "positioning", columnDefinition = "TEXT")
    private String positioning;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @Column(name = "products_summary", columnDefinition = "TEXT")
    private String productsSummary;

    @Column(name = "services_summary", columnDefinition = "TEXT")
    private String servicesSummary;

    @Column(name = "usp", columnDefinition = "TEXT")
    private String usp;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "brand_voice", length = 100)
    private String brandVoice;

    @Column(name = "writing_style", columnDefinition = "TEXT")
    private String writingStyle;

    @Column(name = "words_to_use", columnDefinition = "TEXT")
    private String wordsToUse;

    @Column(name = "words_to_avoid", columnDefinition = "TEXT")
    private String wordsToAvoid;

    @Column(name = "content_goals", columnDefinition = "TEXT")
    private String contentGoals;

    @Column(name = "brand_guidelines", columnDefinition = "TEXT")
    private String brandGuidelines;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
