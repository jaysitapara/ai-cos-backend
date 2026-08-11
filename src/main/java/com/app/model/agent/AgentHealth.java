package com.app.model.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentHealth {
    private boolean available;
    private Instant lastHeartbeat;
    private long successCount;
    private long failureCount;
    private double successRate;
    private double avgExecutionTimeMs;
}
