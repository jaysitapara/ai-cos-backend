package com.app.model.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackPlanModel {
    private String rollbackId;
    private String deploymentId;
    private String targetVersion;
    private String rollbackType; // DEPLOYMENT, CONFIG, DATABASE, FULL
    private String status; // READY, EXECUTED, FAILED
    private Instant timestamp;
}
