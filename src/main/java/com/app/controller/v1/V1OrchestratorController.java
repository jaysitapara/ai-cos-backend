package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.orchestrator.ExecutionContext;
import com.app.model.orchestrator.OrchestratorEvent;
import com.app.service.OrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/orchestrator/executions")
@RequiredArgsConstructor
@Tag(name = "V1 Orchestrator", description = "AI Orchestrator Execution APIs")
public class V1OrchestratorController {

    private final OrchestratorService orchestratorService;

    @PostMapping
    @Operation(summary = "Submit goal and open execution session")
    public ResponseEntity<ExecutionContext> createExecution(@RequestBody Map<String, Object> body) {
        String goalPrompt = (String) body.get("goalPrompt");
        String conversationIdStr = (String) body.get("conversationId");
        UUID conversationId = conversationIdStr != null ? UUID.fromString(conversationIdStr) : UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        List<String> attachmentIds = (List<String>) body.get("attachmentIds");

        ExecutionContext context = orchestratorService.createExecution(userId, workspaceId, conversationId, goalPrompt, attachmentIds);
        return ResponseEntity.ok(context);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get execution details")
    public ResponseEntity<ExecutionContext> getExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(orchestratorService.getExecution(id));
    }

    @GetMapping
    @Operation(summary = "List execution sessions")
    public ResponseEntity<List<ExecutionContext>> listExecutions() {
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(orchestratorService.listExecutions(userId));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause execution")
    public ResponseEntity<ExecutionContext> pauseExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(orchestratorService.pauseExecution(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume execution")
    public ResponseEntity<ExecutionContext> resumeExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(orchestratorService.resumeExecution(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel execution")
    public ResponseEntity<ExecutionContext> cancelExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(orchestratorService.cancelExecution(id));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get execution lifecycle event stream")
    public ResponseEntity<List<OrchestratorEvent>> getEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(orchestratorService.getEvents(id));
    }
}
