package com.app.service;

import com.app.enums.AgentCategory;
import com.app.enums.AgentStatus;
import com.app.model.agent.AgentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
        registry.initDefaultAgents();
    }

    @Test
    void testRegisterAndResolveAgent() {
        AgentMetadata customAgent = AgentMetadata.builder()
            .agentId("agent-backend-01")
            .name("Backend Agent")
            .category(AgentCategory.BACKEND)
            .capabilities(List.of("java_spring_boot", "api_design"))
            .status(AgentStatus.READY)
            .priority(1)
            .build();

        registry.registerAgent(customAgent);

        Optional<AgentMetadata> found = registry.getAgent("agent-backend-01");
        assertTrue(found.isPresent());
        assertEquals("Backend Agent", found.get().getName());

        List<AgentMetadata> resolved = registry.resolveCapabilities(List.of("java_spring_boot"));
        assertEquals(1, resolved.size());
        assertEquals("agent-backend-01", resolved.get(0).getAgentId());
    }

    @Test
    void testUpdateAgentStatus() {
        AgentMetadata updated = registry.updateStatus("agent-planner-01", AgentStatus.DISABLED);
        assertEquals(AgentStatus.DISABLED, updated.getStatus());
    }
}
