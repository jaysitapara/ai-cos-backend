package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.architecture.ApiEndpointSpec;
import com.app.model.architecture.EntitySchema;
import com.app.model.architecture.TechnicalBlueprint;
import com.app.service.ArchitectureGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/architecture")
@RequiredArgsConstructor
@Tag(name = "V1 Architecture Engine", description = "System Architecture, ER Diagram & API Specs REST APIs")
public class V1ArchitectureController {

    private final ArchitectureGeneratorService architectureGeneratorService;

    @PostMapping("/generate")
    @Operation(summary = "Generate technical blueprint, database schema, and API specs")
    public ResponseEntity<TechnicalBlueprint> generateBlueprint(@RequestBody Map<String, String> body) {
        String specId = body.getOrDefault("specId", "spec-default");
        return ResponseEntity.ok(architectureGeneratorService.generateBlueprint(specId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get technical blueprint by ID")
    public ResponseEntity<TechnicalBlueprint> getBlueprint(@PathVariable String id) {
        return architectureGeneratorService.getBlueprint(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/database")
    @Operation(summary = "List ER database entities for a technical blueprint")
    public ResponseEntity<List<EntitySchema>> getDatabaseSchemas(@PathVariable String id) {
        return architectureGeneratorService.getBlueprint(id)
            .map(TechnicalBlueprint::getEntities)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/apis")
    @Operation(summary = "List API endpoint specifications for a technical blueprint")
    public ResponseEntity<List<ApiEndpointSpec>> getApiSpecs(@PathVariable String id) {
        return architectureGeneratorService.getBlueprint(id)
            .map(TechnicalBlueprint::getApiEndpoints)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
