package com.app.model.crossproject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossProjectRecommendation {
    private String recommendationId;
    private String sourceProjectId;
    private String targetProjectId;
    private String type; // REUSABLE_MODULE, ARCHITECTURE_PATTERN, SHARED_API
    private String title;
    private String description;
    private double confidenceScore;
    private String rationale;
}
