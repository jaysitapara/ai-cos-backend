package com.app.entity;

import com.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_execution_graph")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionTaskEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AgentWorkspaceSessionEntity session;

    @Column(name = "task_key", nullable = false, length = 100)
    private String taskKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "agent_role", nullable = false, length = 100)
    private String agentRole;

    @Column(name = "phase", nullable = false, length = 100)
    private String phase;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(name = "dependencies_json", columnDefinition = "TEXT")
    private String dependenciesJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "retries", nullable = false)
    private Integer retries;

    @Column(name = "reasoning_summary", columnDefinition = "TEXT")
    private String reasoningSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
