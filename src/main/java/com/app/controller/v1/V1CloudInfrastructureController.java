package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.cloud.CloudEnvironmentModel;
import com.app.model.cloud.InfrastructureTemplate;
import com.app.service.CloudInfrastructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/cloud-infrastructure")
@RequiredArgsConstructor
@Tag(name = "V1 Cloud Infrastructure Engine", description = "Cloud Environment Provisioning & IaC REST APIs")
public class V1CloudInfrastructureController {

    private final CloudInfrastructureService cloudInfrastructureService;

    @PostMapping("/environments")
    @Operation(summary = "Create and provision target cloud environment plan")
    public ResponseEntity<CloudEnvironmentModel> createEnvironment(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "staging-env");
        String type = body.getOrDefault("type", "STAGING");
        String provider = body.getOrDefault("provider", "AWS");
        String region = body.getOrDefault("region", "us-east-1");

        return ResponseEntity.ok(cloudInfrastructureService.createEnvironment(name, type, provider, region));
    }

    @GetMapping("/environments/{id}")
    @Operation(summary = "Get cloud environment status by ID")
    public ResponseEntity<CloudEnvironmentModel> getEnvironment(@PathVariable String id) {
        return cloudInfrastructureService.getEnvironment(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/environments/{id}/template")
    @Operation(summary = "Get generated IaC infrastructure blueprint template")
    public ResponseEntity<InfrastructureTemplate> getTemplate(@PathVariable String id) {
        return cloudInfrastructureService.getTemplate(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
