package com.app.model.pipeline;

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
public class ReleaseValidationReport {
    private String releaseId;
    private String specId;
    private String blueprintId;
    private List<QualityGate> qualityGates;
    private double productionReadinessScore;
    private boolean approved;
    private Instant timestamp;
}
