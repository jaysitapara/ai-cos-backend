package com.app.service;

import com.app.enums.AgentMessageType;
import com.app.model.communication.AgentMessageContract;
import com.app.model.communication.ContextSnapshot;
import com.app.model.orchestrator.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextManagerServiceTest {

    private ContextManager contextManager;
    private ContextManagerService contextManagerService;
    private InternalMessageBus messageBus;

    @BeforeEach
    void setUp() {
        contextManager = new ContextManager();
        contextManagerService = new ContextManagerService(contextManager);
        messageBus = new InternalMessageBus();
    }

    @Test
    void testTakeSnapshotAndMessageBus() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        ExecutionContext ctx = contextManager.createContext(userId, workspaceId, conversationId, "Shared Context Test Goal", List.of());
        ContextSnapshot snap1 = contextManagerService.takeSnapshot(ctx.getExecutionId());

        assertNotNull(snap1);
        assertEquals(1, snap1.getVersion());
        assertEquals(1, contextManagerService.getHistory(ctx.getExecutionId()).size());

        // Publish Message to Bus
        AgentMessageContract msg = AgentMessageContract.builder()
            .executionId(ctx.getExecutionId())
            .senderAgentId("agent-planner-01")
            .recipientAgentId("agent-qa-01")
            .type(AgentMessageType.REQUEST)
            .payload(Map.of("task", "verify_plan"))
            .build();

        AgentMessageContract published = messageBus.publish(msg);
        assertNotNull(published.getMessageId());
        assertEquals(1, messageBus.getMessages(ctx.getExecutionId()).size());
    }
}
