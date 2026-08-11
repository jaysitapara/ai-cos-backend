package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.communication.ContextSnapshot;
import com.app.model.orchestrator.ExecutionContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContextManagerService {

    private final ContextManager contextManager;
    private final Map<UUID, List<ContextSnapshot>> snapshotStore = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> versionTracker = new ConcurrentHashMap<>();

    public ContextManagerService(ContextManager contextManager) {
        this.contextManager = contextManager;
    }

    public ContextSnapshot takeSnapshot(UUID executionId) {
        ExecutionContext context = contextManager.getContext(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Context not found for execution: " + executionId));

        int nextVersion = versionTracker.getOrDefault(executionId, 0) + 1;
        versionTracker.put(executionId, nextVersion);

        ContextSnapshot snapshot = ContextSnapshot.builder()
            .snapshotId("snap-" + UUID.randomUUID())
            .executionId(executionId)
            .version(nextVersion)
            .goalPrompt(context.getGoalPrompt())
            .requirements(List.of("Verify environment capabilities", "Check attachment compliance"))
            .assumptions(context.getAssumptions())
            .constraints(List.of("Must complete within timeout limit", "No secret disclosure"))
            .status(context.getStatus())
            .timestamp(Instant.now())
            .build();

        snapshotStore.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>())).add(snapshot);
        return snapshot;
    }

    public List<ContextSnapshot> getHistory(UUID executionId) {
        return snapshotStore.getOrDefault(executionId, List.of());
    }
}
