package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.orchestrator.ExecutionContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContextManager {

    private final Map<UUID, ExecutionContext> contextStore = new ConcurrentHashMap<>();

    public ExecutionContext createContext(UUID userId, UUID workspaceId, UUID conversationId, String goalPrompt, List<String> attachmentIds) {
        UUID executionId = UUID.randomUUID();
        ExecutionContext context = ExecutionContext.builder()
            .executionId(executionId)
            .userId(userId)
            .workspaceId(workspaceId)
            .conversationId(conversationId)
            .goalPrompt(goalPrompt)
            .attachmentIds(attachmentIds != null ? attachmentIds : List.of())
            .assumptions(List.of("Assumes standard workspace configuration", "Targeting primary environment"))
            .configuration(Map.of("maxParallelTasks", 3))
            .status(ExecutionState.CREATED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        contextStore.put(executionId, context);
        return context;
    }

    public Optional<ExecutionContext> getContext(UUID executionId) {
        return Optional.ofNullable(contextStore.get(executionId));
    }

    public List<ExecutionContext> listUserContexts(UUID userId) {
        return contextStore.values().stream()
            .filter(c -> c.getUserId() != null && c.getUserId().equals(userId))
            .sorted(Comparator.comparing(ExecutionContext::getCreatedAt).reversed())
            .toList();
    }

    public void updateStatus(UUID executionId, ExecutionState status) {
        ExecutionContext context = contextStore.get(executionId);
        if (context != null) {
            context.setStatus(status);
            context.setUpdatedAt(Instant.now());
        }
    }
}
