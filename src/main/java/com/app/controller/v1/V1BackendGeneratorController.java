package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.backendgen.BackendPlan;
import com.app.service.BackendGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/backend-generator")
@RequiredArgsConstructor
@Tag(name = "V1 Backend Generator Engine", description = "Backend Architecture, Test Strategy & Docs REST APIs")
public class V1BackendGeneratorController {

    private final BackendGeneratorService backendGeneratorService;

    @PostMapping("/generate")
    @Operation(summary = "Generate backend implementation plan, testing strategy, and documentation")
    public ResponseEntity<BackendPlan> generateBackendPlan(@RequestBody Map<String, String> body) {
        String blueprintId = body.getOrDefault("blueprintId", "blue-default");
        return ResponseEntity.ok(backendGeneratorService.generateBackendPlan(blueprintId));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get backend implementation plan by ID")
    public ResponseEntity<BackendPlan> getPlan(@PathVariable String id) {
        return backendGeneratorService.getPlan(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plans/{id}/docs")
    @Operation(summary = "Get generated documentation artifacts (README, API guide, deployment notes)")
    public ResponseEntity<Map<String, String>> getDocs(@PathVariable String id) {
        return backendGeneratorService.getPlan(id)
            .map(BackendPlan::getDocumentationArtifacts)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
