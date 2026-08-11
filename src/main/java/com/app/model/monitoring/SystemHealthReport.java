package com.app.model.monitoring;

import com.app.enums.HealthLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthReport {
    private HealthLevel overallStatus;
    private HealthLevel executionHealth;
    private HealthLevel workflowHealth;
    private HealthLevel queueHealth;
    private HealthLevel providerHealth;
    private int activeExecutionsCount;
    private int stalledExecutionsCount;
    private int queueDepth;
    private double successRatePercent;
    private Map<String, Object> metrics;
    private Instant timestamp;
}
