package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_briefs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBriefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ContentThreadEntity thread;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "topic", columnDefinition = "TEXT")
    private String topic;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @Column(name = "key_message", columnDefinition = "TEXT")
    private String keyMessage;

    @Column(name = "tone", length = 100)
    private String tone;

    @Column(name = "cta", columnDefinition = "TEXT")
    private String cta;

    @Column(name = "additional_instructions", columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(name = "structured_brief_json", nullable = false, columnDefinition = "TEXT")
    private String structuredBriefJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
