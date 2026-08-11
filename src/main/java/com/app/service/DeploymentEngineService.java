package com.app.service;

import com.app.enums.DeploymentStrategy;
import com.app.enums.TargetEnvironment;
import com.app.model.deployment.DeploymentJobModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeploymentEngineService {

    private final Map<String, DeploymentJobModel> deploymentStore = new ConcurrentHashMap<>();

    public DeploymentJobModel createDeployment(String buildId, TargetEnvironment env, DeploymentStrategy strategy) {
        String depId = "dep-" + UUID.randomUUID();
        TargetEnvironment targetEnv = env != null ? env : TargetEnvironment.STAGING;

        DeploymentJobModel dep = DeploymentJobModel.builder()
            .deploymentId(depId)
            .executionId(UUID.randomUUID())
            .buildId(buildId)
            .environment(targetEnv)
            .strategy(strategy != null ? strategy : DeploymentStrategy.BLUE_GREEN)
            .status("SUCCESS")
            .liveUrl("https://" + targetEnv.name().toLowerCase() + ".ai-cos.internal")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        deploymentStore.put(depId, dep);
        return dep;
    }

    public Optional<DeploymentJobModel> getDeployment(String depId) {
        return Optional.ofNullable(deploymentStore.get(depId));
    }

    public DeploymentJobModel promoteEnvironment(String depId, TargetEnvironment newEnv) {
        DeploymentJobModel dep = deploymentStore.get(depId);
        if (dep == null) {
            throw new IllegalArgumentException("Deployment not found: " + depId);
        }
        dep.setEnvironment(newEnv);
        dep.setLiveUrl("https://" + newEnv.name().toLowerCase() + ".ai-cos.internal");
        dep.setUpdatedAt(Instant.now());
        return dep;
    }
}
