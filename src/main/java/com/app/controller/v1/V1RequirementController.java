package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.requirement.RequirementAnalysisResult;
import com.app.model.requirement.RequirementItem;
import com.app.service.RequirementAnalyzer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/requirements")
@RequiredArgsConstructor
@Tag(name = "V1 Requirements Engine", description = "Requirement Analysis Engine REST APIs")
public class V1RequirementController {

    private final RequirementAnalyzer requirementAnalyzer;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze goal and extract software requirements")
    public ResponseEntity<RequirementAnalysisResult> analyzeGoal(@RequestBody Map<String, Object> body) {
        String goalPrompt = (String) body.getOrDefault("goalPrompt", "Standard Software Goal");
        @SuppressWarnings("unchecked")
        List<String> attachmentIds = (List<String>) body.get("attachmentIds");

        return ResponseEntity.ok(requirementAnalyzer.analyzeGoal(goalPrompt, attachmentIds));
    }

    @GetMapping("/analysis/{id}")
    @Operation(summary = "Get requirement analysis result by ID")
    public ResponseEntity<RequirementAnalysisResult> getAnalysis(@PathVariable String id) {
        return requirementAnalyzer.getAnalysis(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis/{id}/items")
    @Operation(summary = "List categorized requirement items for an analysis")
    public ResponseEntity<List<RequirementItem>> getRequirementItems(@PathVariable String id) {
        return requirementAnalyzer.getAnalysis(id)
            .map(RequirementAnalysisResult::getRequirements)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/analysis/{id}/assumptions")
    @Operation(summary = "Update assumptions for a requirement analysis")
    public ResponseEntity<RequirementAnalysisResult> updateAssumptions(
            @PathVariable String id,
            @RequestBody List<String> assumptions) {
        return ResponseEntity.ok(requirementAnalyzer.updateAssumptions(id, assumptions));
    }
}
