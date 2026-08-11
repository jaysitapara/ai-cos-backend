package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.orchestrator.ExecutionContext;
import com.app.model.orchestrator.OrchestratorEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrchestratorService {

    private final ContextManager contextManager;
    private final ExecutionCoordinator coordinator;
    private final EventDispatcher eventDispatcher;

    public OrchestratorService(ContextManager contextManager, ExecutionCoordinator coordinator, EventDispatcher eventDispatcher) {
        this.contextManager = contextManager;
        this.coordinator = coordinator;
        this.eventDispatcher = eventDispatcher;
    }

    public ExecutionContext createExecution(UUID userId, UUID workspaceId, UUID conversationId, String goalPrompt, List<String> attachmentIds) {
        if (goalPrompt == null || goalPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal prompt cannot be empty.");
        }

        ExecutionContext context = contextManager.createContext(userId, workspaceId, conversationId, goalPrompt, attachmentIds);
        eventDispatcher.dispatchEvent(context.getExecutionId(), "ExecutionCreated", ExecutionState.CREATED, "Execution context created.");

        // Automatically transition to Planning phase
        coordinator.transitionState(context.getExecutionId(), ExecutionState.PLANNING, "Drafting execution plan.");
        coordinator.transitionState(context.getExecutionId(), ExecutionState.WAITING_APPROVAL, "Execution plan drafted. Awaiting user authorization.");
        return context;
    }

    public ExecutionContext getExecution(UUID executionId) {
        return contextManager.getContext(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
    }

    public List<ExecutionContext> listExecutions(UUID userId) {
        return contextManager.listUserContexts(userId);
    }

    public ExecutionContext pauseExecution(UUID executionId) {
        return coordinator.transitionState(executionId, ExecutionState.PAUSED, "Execution paused by user.");
    }

    public ExecutionContext resumeExecution(UUID executionId) {
        return coordinator.transitionState(executionId, ExecutionState.RUNNING, "Execution resumed by user.");
    }

    public ExecutionContext cancelExecution(UUID executionId) {
        return coordinator.transitionState(executionId, ExecutionState.CANCELLED, "Execution cancelled by user.");
    }

    public List<OrchestratorEvent> getEvents(UUID executionId) {
        return eventDispatcher.getEvents(executionId);
    }
}
