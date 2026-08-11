package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.build.BuildJobModel;
import com.app.service.BuildOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/builds")
@RequiredArgsConstructor
@Tag(name = "V1 Build Engine", description = "Build System & CI/CD Pipeline REST APIs")
public class V1BuildController {

    private final BuildOrchestrationService buildOrchestrationService;

    @PostMapping
    @Operation(summary = "Create and trigger build orchestration job")
    public ResponseEntity<BuildJobModel> createBuild(@RequestBody Map<String, String> body) {
        String repoId = body.getOrDefault("repositoryId", "repo-default");
        String branch = body.getOrDefault("branch", "main");
        return ResponseEntity.ok(buildOrchestrationService.createBuild(repoId, branch));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get build details by ID")
    public ResponseEntity<BuildJobModel> getBuild(@PathVariable String id) {
        return buildOrchestrationService.getBuild(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pipeline")
    @Operation(summary = "Get generated CI/CD YAML pipeline specification")
    public ResponseEntity<String> getPipelineYaml(@PathVariable String id) {
        return buildOrchestrationService.getBuild(id)
            .map(BuildJobModel::getPipelineYaml)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/artifacts")
    @Operation(summary = "List build output artifacts")
    public ResponseEntity<List<String>> getArtifacts(@PathVariable String id) {
        return buildOrchestrationService.getBuild(id)
            .map(BuildJobModel::getArtifacts)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
