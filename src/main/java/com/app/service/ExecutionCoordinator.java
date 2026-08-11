package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.orchestrator.ExecutionContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExecutionCoordinator {

    private final ContextManager contextManager;
    private final EventDispatcher eventDispatcher;

    public ExecutionCoordinator(ContextManager contextManager, EventDispatcher eventDispatcher) {
        this.contextManager = contextManager;
        this.eventDispatcher = eventDispatcher;
    }

    public ExecutionContext transitionState(UUID executionId, ExecutionState targetState, String reason) {
        ExecutionContext context = contextManager.getContext(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Execution context not found for ID: " + executionId));

        contextManager.updateStatus(executionId, targetState);
        eventDispatcher.dispatchEvent(executionId, "StateTransition_" + targetState.name(), targetState, reason);
        return context;
    }
}
