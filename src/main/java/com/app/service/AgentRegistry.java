package com.app.service;

import com.app.enums.AgentCategory;
import com.app.enums.AgentStatus;
import com.app.model.agent.AgentHealth;
import com.app.model.agent.AgentMetadata;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentRegistry {

    private final Map<String, AgentMetadata> registry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaultAgents() {
        registerAgent(AgentMetadata.builder()
            .agentId("agent-planner-01")
            .name("Planning Agent")
            .description("Deconstructs goals into structured execution steps")
            .version("1.0.0")
            .category(AgentCategory.PLANNING)
            .capabilities(List.of("goal_decomposition", "task_planning", "step_ordering"))
            .inputTypes(List.of("text/plain", "application/json"))
            .outputTypes(List.of("application/json"))
            .requiredPermissions(List.of("READ_WORKSPACE"))
            .status(AgentStatus.READY)
            .priority(1)
            .health(AgentHealth.builder()
                .available(true)
                .lastHeartbeat(Instant.now())
                .successCount(42)
                .failureCount(0)
                .successRate(100.0)
                .avgExecutionTimeMs(120.0)
                .build())
            .build());

        registerAgent(AgentMetadata.builder()
            .agentId("agent-qa-01")
            .name("QA & Verification Agent")
            .description("Validates outputs and checks code/artifact compliance")
            .version("1.0.0")
            .category(AgentCategory.QA)
            .capabilities(List.of("code_verification", "syntax_check", "test_execution"))
            .inputTypes(List.of("text/plain", "application/zip"))
            .outputTypes(List.of("application/json"))
            .requiredPermissions(List.of("EXECUTE_TESTS"))
            .status(AgentStatus.READY)
            .priority(2)
            .health(AgentHealth.builder()
                .available(true)
                .lastHeartbeat(Instant.now())
                .successCount(38)
                .failureCount(1)
                .successRate(97.4)
                .avgExecutionTimeMs(250.0)
                .build())
            .build());
    }

    public AgentMetadata registerAgent(AgentMetadata agent) {
        if (agent.getAgentId() == null || agent.getAgentId().trim().isEmpty()) {
            agent.setAgentId("agent-" + UUID.randomUUID());
        }
        if (agent.getHealth() == null) {
            agent.setHealth(AgentHealth.builder()
                .available(true)
                .lastHeartbeat(Instant.now())
                .successCount(0)
                .failureCount(0)
                .successRate(100.0)
                .avgExecutionTimeMs(0.0)
                .build());
        }
        registry.put(agent.getAgentId(), agent);
        return agent;
    }

    public boolean unregisterAgent(String agentId) {
        return registry.remove(agentId) != null;
    }

    public Optional<AgentMetadata> getAgent(String agentId) {
        return Optional.ofNullable(registry.get(agentId));
    }

    public List<AgentMetadata> listAgents(AgentCategory category, String capability) {
        return registry.values().stream()
            .filter(a -> category == null || a.getCategory() == category)
            .filter(a -> capability == null || (a.getCapabilities() != null && a.getCapabilities().contains(capability)))
            .toList();
    }

    public AgentMetadata updateStatus(String agentId, AgentStatus status) {
        AgentMetadata agent = registry.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        agent.setStatus(status);
        if (agent.getHealth() != null) {
            agent.getHealth().setLastHeartbeat(Instant.now());
        }
        return agent;
    }

    public List<AgentMetadata> resolveCapabilities(List<String> requiredCapabilities) {
        return registry.values().stream()
            .filter(a -> a.getStatus() == AgentStatus.READY || a.getStatus() == AgentStatus.REGISTERED)
            .filter(a -> a.getCapabilities() != null && a.getCapabilities().containsAll(requiredCapabilities))
            .sorted(Comparator.comparingInt(AgentMetadata::getPriority))
            .toList();
    }
}
