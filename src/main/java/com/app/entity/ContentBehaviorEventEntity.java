package com.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_behavior_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentBehaviorEventEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private GeneratedContentEntity content;

    @Column(name = "event_type", nullable = false)
    private String eventType; // CONTENT_GENERATED, CONTENT_EDITED, CONTENT_APPROVED, CONTENT_REJECTED, etc.

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
