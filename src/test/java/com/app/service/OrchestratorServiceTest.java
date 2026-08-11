package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.orchestrator.ExecutionContext;
import com.app.model.orchestrator.OrchestratorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrchestratorServiceTest {

    private OrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        ContextManager contextManager = new ContextManager();
        EventDispatcher eventDispatcher = new EventDispatcher();
        ExecutionCoordinator coordinator = new ExecutionCoordinator(contextManager, eventDispatcher);
        orchestratorService = new OrchestratorService(contextManager, coordinator, eventDispatcher);
    }

    @Test
    void testCreateAndManageExecutionLifecycle() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String goal = "Analyze competitor pricing and generate strategy report";

        ExecutionContext context = orchestratorService.createExecution(userId, workspaceId, conversationId, goal, List.of("att-1"));

        assertNotNull(context);
        assertEquals(goal, context.getGoalPrompt());
        assertEquals(ExecutionState.WAITING_APPROVAL, context.getStatus());

        // Resume & Pause
        ExecutionContext resumed = orchestratorService.resumeExecution(context.getExecutionId());
        assertEquals(ExecutionState.RUNNING, resumed.getStatus());

        ExecutionContext paused = orchestratorService.pauseExecution(context.getExecutionId());
        assertEquals(ExecutionState.PAUSED, paused.getStatus());

        // Cancel
        ExecutionContext cancelled = orchestratorService.cancelExecution(context.getExecutionId());
        assertEquals(ExecutionState.CANCELLED, cancelled.getStatus());

        // Check Event Stream
        List<OrchestratorEvent> events = orchestratorService.getEvents(context.getExecutionId());
        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> "ExecutionCreated".equals(e.getEventType())));
    }
}
