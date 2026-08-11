package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.enums.AgentCategory;
import com.app.enums.AgentStatus;
import com.app.model.agent.AgentHealth;
import com.app.model.agent.AgentMetadata;
import com.app.service.AgentRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/agents")
@RequiredArgsConstructor
@Tag(name = "V1 Agents", description = "Agent Registry & Lifecycle REST APIs")
public class V1AgentController {

    private final AgentRegistry agentRegistry;

    @PostMapping
    @Operation(summary = "Register a new AI Agent")
    public ResponseEntity<AgentMetadata> registerAgent(@RequestBody AgentMetadata agent) {
        return ResponseEntity.ok(agentRegistry.registerAgent(agent));
    }

    @GetMapping
    @Operation(summary = "List registered AI agents with category and capability filters")
    public ResponseEntity<List<AgentMetadata>> listAgents(
            @RequestParam(value = "category", required = false) AgentCategory category,
            @RequestParam(value = "capability", required = false) String capability) {
        return ResponseEntity.ok(agentRegistry.listAgents(category, capability));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent details by ID")
    public ResponseEntity<AgentMetadata> getAgent(@PathVariable String id) {
        return agentRegistry.getAgent(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update agent lifecycle status (ENABLE/DISABLE/READY)")
    public ResponseEntity<AgentMetadata> updateAgentStatus(
            @PathVariable String id,
            @RequestParam AgentStatus status) {
        return ResponseEntity.ok(agentRegistry.updateStatus(id, status));
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "Get agent health metrics and heartbeat")
    public ResponseEntity<AgentHealth> getAgentHealth(@PathVariable String id) {
        return agentRegistry.getAgent(id)
            .map(AgentMetadata::getHealth)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolve capable agents matching required capabilities")
    public ResponseEntity<List<AgentMetadata>> resolveCapabilities(@RequestParam List<String> capabilities) {
        return ResponseEntity.ok(agentRegistry.resolveCapabilities(capabilities));
    }
}
