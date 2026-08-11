package com.app.service;

import com.app.model.communication.AgentMessageContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InternalMessageBus {

    private static final Logger log = LoggerFactory.getLogger(InternalMessageBus.class);
    private final Map<UUID, List<AgentMessageContract>> messageStore = new ConcurrentHashMap<>();

    public AgentMessageContract publish(AgentMessageContract message) {
        if (message.getMessageId() == null) {
            message.setMessageId("msg-" + UUID.randomUUID());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(Instant.now());
        }

        messageStore.computeIfAbsent(message.getExecutionId(), k -> Collections.synchronizedList(new ArrayList<>())).add(message);
        log.info("Communication Bus: AgentMessage published [{}] from {} to {} (Execution: {})",
            message.getType(), message.getSenderAgentId(), message.getRecipientAgentId() != null ? message.getRecipientAgentId() : "BROADCAST", message.getExecutionId());

        return message;
    }

    public List<AgentMessageContract> getMessages(UUID executionId) {
        return messageStore.getOrDefault(executionId, List.of());
    }
}
