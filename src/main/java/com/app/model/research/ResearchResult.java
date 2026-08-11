package com.app.model.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchResult {
    private String researchId;
    private String analysisId;
    private String domainClassification;
    private List<String> technicalRecommendations;
    private List<RiskItem> riskAnalysis;
    private List<Map<String, Object>> milestones;
    private int estimatedEffortDays;
    private Instant createdAt;
}
