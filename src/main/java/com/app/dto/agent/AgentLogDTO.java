package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLogDTO {
    private Long id;
    private Long taskId;
    private String agentRole;
    private String logLevel;
    private String message;
    private String reasoning;
    private OffsetDateTime timestamp;
}
