package com.app.model.communication;

import com.app.enums.AgentMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageContract {
    private String messageId;
    private UUID executionId;
    private String senderAgentId;
    private String recipientAgentId; // Null if broadcast
    private AgentMessageType type;
    private Map<String, Object> payload;
    private Instant timestamp;
}
