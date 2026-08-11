package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "job_id", length = 100)
    private String jobId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "request_type", length = 50)
    private String requestType;

    @Column(name = "agent_role", length = 50)
    private String agentRole;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
