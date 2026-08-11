package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.research.ResearchResult;
import com.app.model.research.RiskItem;
import com.app.service.ResearchEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/research")
@RequiredArgsConstructor
@Tag(name = "V1 Research Engine", description = "Research & Technical Strategy REST APIs")
public class V1ResearchController {

    private final ResearchEngine researchEngine;

    @PostMapping("/start")
    @Operation(summary = "Start domain research and strategy formulation for requirements")
    public ResponseEntity<ResearchResult> startResearch(@RequestBody Map<String, String> body) {
        String analysisId = body.getOrDefault("analysisId", "req-default");
        return ResponseEntity.ok(researchEngine.executeResearch(analysisId));
    }

    @GetMapping("/results/{id}")
    @Operation(summary = "Get research results by ID")
    public ResponseEntity<ResearchResult> getResearchResult(@PathVariable String id) {
        return researchEngine.getResult(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/results/{id}/risks")
    @Operation(summary = "List risk analysis and mitigation plan")
    public ResponseEntity<List<RiskItem>> getRisks(@PathVariable String id) {
        return researchEngine.getResult(id)
            .map(ResearchResult::getRiskAnalysis)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/results/{id}/recommendations")
    @Operation(summary = "List technical strategy recommendations")
    public ResponseEntity<List<String>> getRecommendations(@PathVariable String id) {
        return researchEngine.getResult(id)
            .map(ResearchResult::getTechnicalRecommendations)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
