package com.app.model.deployment;

import com.app.enums.DeploymentStrategy;
import com.app.enums.TargetEnvironment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentJobModel {
    private String deploymentId;
    private UUID executionId;
    private String buildId;
    private TargetEnvironment environment;
    private DeploymentStrategy strategy;
    private String status; // SUCCESS, PENDING, FAILED, CANCELLED
    private String liveUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
