package com.app.model.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewReport {
    private String reportId;
    private String planId;
    private double overallQualityScore;
    private double securityScore;
    private double performanceScore;
    private double maintainabilityScore;
    private List<CodeReviewIssue> issues;
    private List<String> optimizationSuggestions;
    private Instant createdAt;
}
