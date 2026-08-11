package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.enums.DeploymentStrategy;
import com.app.enums.TargetEnvironment;
import com.app.model.deployment.DeploymentJobModel;
import com.app.service.DeploymentEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/deployments")
@RequiredArgsConstructor
@Tag(name = "V1 Deployment Engine", description = "Autonomous Deployment & Environment Promotion REST APIs")
public class V1DeploymentController {

    private final DeploymentEngineService deploymentEngineService;

    @PostMapping
    @Operation(summary = "Create and trigger autonomous deployment execution")
    public ResponseEntity<DeploymentJobModel> createDeployment(@RequestBody Map<String, String> body) {
        String buildId = body.getOrDefault("buildId", "bld-default");
        String envStr = body.getOrDefault("environment", "STAGING");
        String stratStr = body.getOrDefault("strategy", "BLUE_GREEN");

        TargetEnvironment env = TargetEnvironment.valueOf(envStr);
        DeploymentStrategy strategy = DeploymentStrategy.valueOf(stratStr);

        return ResponseEntity.ok(deploymentEngineService.createDeployment(buildId, env, strategy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deployment status by ID")
    public ResponseEntity<DeploymentJobModel> getDeployment(@PathVariable String id) {
        return deploymentEngineService.getDeployment(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/promote")
    @Operation(summary = "Promote deployment to target environment (e.g. STAGING -> PRODUCTION)")
    public ResponseEntity<DeploymentJobModel> promoteEnvironment(@PathVariable String id, @RequestBody Map<String, String> body) {
        String targetEnvStr = body.getOrDefault("targetEnvironment", "PRODUCTION");
        TargetEnvironment targetEnv = TargetEnvironment.valueOf(targetEnvStr);

        return ResponseEntity.ok(deploymentEngineService.promoteEnvironment(id, targetEnv));
    }
}
