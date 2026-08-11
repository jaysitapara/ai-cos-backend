package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.frontend.FrontendPlan;
import com.app.model.frontend.UiComponentSpec;
import com.app.service.FrontendGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/frontend-generator")
@RequiredArgsConstructor
@Tag(name = "V1 Frontend Generator Engine", description = "UI/UX Specs & Component Hierarchy REST APIs")
public class V1FrontendGeneratorController {

    private final FrontendGeneratorService frontendGeneratorService;

    @PostMapping("/generate")
    @Operation(summary = "Generate frontend plan, route structure, design system, and component hierarchy")
    public ResponseEntity<FrontendPlan> generateFrontendPlan(@RequestBody Map<String, String> body) {
        String blueprintId = body.getOrDefault("blueprintId", "blue-default");
        return ResponseEntity.ok(frontendGeneratorService.generateFrontendPlan(blueprintId));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get frontend plan by ID")
    public ResponseEntity<FrontendPlan> getPlan(@PathVariable String id) {
        return frontendGeneratorService.getPlan(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plans/{id}/components")
    @Operation(summary = "List component specifications for a frontend plan")
    public ResponseEntity<List<UiComponentSpec>> getComponents(@PathVariable String id) {
        return frontendGeneratorService.getPlan(id)
            .map(FrontendPlan::getComponents)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
