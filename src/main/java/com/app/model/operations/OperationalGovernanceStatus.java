package com.app.model.operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalGovernanceStatus {
    private String governanceId;
    private int incidentCount;
    private double complianceScore;
    private int auditTrailCount;
    private String disasterRecoveryStatus; // READY, SYNCED, DEGRADED
    private Instant timestamp;
}
