package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "approved_contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "final_content", nullable = false, columnDefinition = "TEXT")
    private String finalContent;

    @Column(name = "performance_rating")
    private Integer performanceRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (performanceRating == null) performanceRating = 5;
    }
}
