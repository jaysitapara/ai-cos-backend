package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.review.CodeReviewIssue;
import com.app.model.review.CodeReviewReport;
import com.app.service.CodeReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/code-review")
@RequiredArgsConstructor
@Tag(name = "V1 Code Review Engine", description = "Code Quality, Security & Optimization Review REST APIs")
public class V1CodeReviewController {

    private final CodeReviewService codeReviewService;

    @PostMapping("/start")
    @Operation(summary = "Run static and AI code review on generated plans")
    public ResponseEntity<CodeReviewReport> startReview(@RequestBody Map<String, String> body) {
        String planId = body.getOrDefault("planId", "plan-default");
        return ResponseEntity.ok(codeReviewService.runCodeReview(planId));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get code review report by ID")
    public ResponseEntity<CodeReviewReport> getReport(@PathVariable String id) {
        return codeReviewService.getReport(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/{id}/issues")
    @Operation(summary = "List detected review issues and bugs")
    public ResponseEntity<List<CodeReviewIssue>> getIssues(@PathVariable String id) {
        return codeReviewService.getReport(id)
            .map(CodeReviewReport::getIssues)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/{id}/metrics")
    @Operation(summary = "Get quality, security, and performance scores summary")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable String id) {
        return codeReviewService.getReport(id)
            .map(r -> Map.<String, Object>of(
                "overallQualityScore", r.getOverallQualityScore(),
                "securityScore", r.getSecurityScore(),
                "performanceScore", r.getPerformanceScore(),
                "maintainabilityScore", r.getMaintainabilityScore()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
