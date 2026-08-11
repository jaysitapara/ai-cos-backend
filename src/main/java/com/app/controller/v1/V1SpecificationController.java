package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.specification.SpecificationBundle;
import com.app.model.specification.UserStoryItem;
import com.app.service.SpecificationGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/specifications")
@RequiredArgsConstructor
@Tag(name = "V1 Specification Engine", description = "BRD, PRD & User Story REST APIs")
public class V1SpecificationController {

    private final SpecificationGenerator specificationGenerator;

    @PostMapping("/generate")
    @Operation(summary = "Generate BRD, PRD & User Stories for requirement analysis")
    public ResponseEntity<SpecificationBundle> generateSpecifications(@RequestBody Map<String, String> body) {
        String analysisId = body.getOrDefault("analysisId", "req-default");
        return ResponseEntity.ok(specificationGenerator.generateSpecifications(analysisId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specification bundle by ID")
    public ResponseEntity<SpecificationBundle> getSpecification(@PathVariable String id) {
        return specificationGenerator.getSpecification(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/user-stories")
    @Operation(summary = "List generated user stories for a specification bundle")
    public ResponseEntity<List<UserStoryItem>> getUserStories(@PathVariable String id) {
        return specificationGenerator.getSpecification(id)
            .map(SpecificationBundle::getUserStories)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/brd")
    @Operation(summary = "Get Business Requirements Document (BRD) markdown")
    public ResponseEntity<String> getBRD(@PathVariable String id) {
        return specificationGenerator.getSpecification(id)
            .map(SpecificationBundle::getBrdContent)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/prd")
    @Operation(summary = "Get Product Requirements Document (PRD) markdown")
    public ResponseEntity<String> getPRD(@PathVariable String id) {
        return specificationGenerator.getSpecification(id)
            .map(SpecificationBundle::getPrdContent)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
