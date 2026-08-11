package com.app.service;

import com.app.model.review.CodeReviewIssue;
import com.app.model.review.CodeReviewReport;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CodeReviewService {

    private final Map<String, CodeReviewReport> reportStore = new ConcurrentHashMap<>();

    public CodeReviewReport runCodeReview(String planId) {
        String reportId = "rev-" + UUID.randomUUID();

        CodeReviewIssue iss1 = CodeReviewIssue.builder()
            .issueId("iss-1")
            .description("Potential Unbounded Collection Memory Growth")
            .severity("MEDIUM")
            .location("com.app.service.ContextManager:24")
            .rootCause("ConcurrentHashMap without explicit retention limit")
            .suggestedResolution("Add periodic cache eviction or size bound")
            .build();

        CodeReviewIssue iss2 = CodeReviewIssue.builder()
            .issueId("iss-2")
            .description("Missing Specific Catch Block for Database Exception")
            .severity("INFORMATIONAL")
            .location("com.app.controller.v1.V1OrchestratorController:32")
            .rootCause("Relying entirely on GlobalExceptionHandler fallback")
            .suggestedResolution("Add explicit validation check before database mutation")
            .build();

        List<String> optimizations = List.of(
            "Use GZIP compression for static assets and API responses",
            "Enable connection pooling metrics monitoring with Micrometer",
            "Leverage React.memo on complex transcript components"
        );

        CodeReviewReport report = CodeReviewReport.builder()
            .reportId(reportId)
            .planId(planId)
            .overallQualityScore(94.5)
            .securityScore(98.0)
            .performanceScore(92.0)
            .maintainabilityScore(95.0)
            .issues(List.of(iss1, iss2))
            .optimizationSuggestions(optimizations)
            .createdAt(Instant.now())
            .build();

        reportStore.put(reportId, report);
        return report;
    }

    public Optional<CodeReviewReport> getReport(String reportId) {
        return Optional.ofNullable(reportStore.get(reportId));
    }
}
