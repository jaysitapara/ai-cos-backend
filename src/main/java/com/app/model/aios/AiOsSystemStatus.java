package com.app.model.aios;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOsSystemStatus {
    private String systemId;
    private String version;
    private int activeGoalsCount;
    private int registeredCapabilitiesCount;
    private int longTermMemoryEntriesCount;
    private String status; // OPERATIONAL, DEGRADED, MAINTENANCE
    private Instant timestamp;
}
