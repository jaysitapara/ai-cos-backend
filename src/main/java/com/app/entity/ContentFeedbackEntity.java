package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_feedbacks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFeedbackEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private GeneratedContentEntity content;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "feedback_type", nullable = false, length = 30)
    private String feedbackType;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback_text", nullable = false, columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (feedbackType == null) feedbackType = "COMMENT";
    }
}
