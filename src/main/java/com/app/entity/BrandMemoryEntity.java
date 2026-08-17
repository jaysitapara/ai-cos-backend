package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "brand_memories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandMemoryEntity {

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

    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    @Column(name = "\"value\"", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (source == null) source = "USER_FEEDBACK";
        if (confidence == null) confidence = 1.0;
    }
}
