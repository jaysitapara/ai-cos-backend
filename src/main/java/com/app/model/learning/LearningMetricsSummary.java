package com.app.model.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMetricsSummary {
    private String metricsId;
    private double recommendationAcceptanceRate;
    private double executionSuccessRate;
    private double averageUserRating;
    private int extractedPatternsCount;
    private Instant updatedAt;
}
