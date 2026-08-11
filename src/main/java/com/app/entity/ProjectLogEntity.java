package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private ProjectJobEntity job;

    @Column(name = "log_level", nullable = false, length = 20)
    private String logLevel;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
        if (logLevel == null) {
            logLevel = "INFO";
        }
        if (source == null) {
            source = "BUILD";
        }
    }
}
