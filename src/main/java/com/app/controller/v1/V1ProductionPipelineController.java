package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.pipeline.QualityGate;
import com.app.model.pipeline.ReleaseValidationReport;
import com.app.service.ProductionPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/production-pipeline")
@RequiredArgsConstructor
@Tag(name = "V1 Production Pipeline Engine", description = "Production Validation & Quality Gate REST APIs")
public class V1ProductionPipelineController {

    private final ProductionPipelineService productionPipelineService;

    @PostMapping("/validate")
    @Operation(summary = "Validate release package readiness against production quality gates")
    public ResponseEntity<ReleaseValidationReport> validateRelease(@RequestBody Map<String, String> body) {
        String specId = body.getOrDefault("specId", "spec-default");
        String blueprintId = body.getOrDefault("blueprintId", "blue-default");
        return ResponseEntity.ok(productionPipelineService.validateRelease(specId, blueprintId));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get release validation report by ID")
    public ResponseEntity<ReleaseValidationReport> getReport(@PathVariable String id) {
        return productionPipelineService.getReport(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/gates")
    @Operation(summary = "List mandatory production quality gates")
    public ResponseEntity<List<QualityGate>> getGates() {
        ReleaseValidationReport sample = productionPipelineService.validateRelease("spec-sample", "blue-sample");
        return ResponseEntity.ok(sample.getQualityGates());
    }
}
