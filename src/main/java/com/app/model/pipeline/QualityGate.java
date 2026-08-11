package com.app.model.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityGate {
    private String gateId;
    private String name;
    private String status; // PASSED, FAILED
    private double score;
    private double targetScore;
    private String description;
}
