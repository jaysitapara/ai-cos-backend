package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.communication.AgentMessageContract;
import com.app.model.communication.ContextSnapshot;
import com.app.model.orchestrator.ExecutionContext;
import com.app.service.ContextManager;
import com.app.service.ContextManagerService;
import com.app.service.InternalMessageBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/context")
@RequiredArgsConstructor
@Tag(name = "V1 Shared Context", description = "Shared Execution Context & Communication Bus REST APIs")
public class V1ContextController {

    private final ContextManager contextManager;
    private final ContextManagerService contextManagerService;
    private final InternalMessageBus messageBus;

    @GetMapping("/{executionId}")
    @Operation(summary = "Get shared execution context")
    public ResponseEntity<ExecutionContext> getContext(@PathVariable UUID executionId) {
        return contextManager.getContext(executionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{executionId}/snapshot")
    @Operation(summary = "Take a context snapshot for versioning and rollback readiness")
    public ResponseEntity<ContextSnapshot> takeSnapshot(@PathVariable UUID executionId) {
        return ResponseEntity.ok(contextManagerService.takeSnapshot(executionId));
    }

    @GetMapping("/{executionId}/history")
    @Operation(summary = "Get context snapshot history")
    public ResponseEntity<List<ContextSnapshot>> getHistory(@PathVariable UUID executionId) {
        return ResponseEntity.ok(contextManagerService.getHistory(executionId));
    }

    @PostMapping("/{executionId}/messages")
    @Operation(summary = "Publish agent message onto internal communication bus")
    public ResponseEntity<AgentMessageContract> publishMessage(@PathVariable UUID executionId, @RequestBody AgentMessageContract message) {
        message.setExecutionId(executionId);
        return ResponseEntity.ok(messageBus.publish(message));
    }

    @GetMapping("/{executionId}/messages")
    @Operation(summary = "List agent communication bus messages")
    public ResponseEntity<List<AgentMessageContract>> getMessages(@PathVariable UUID executionId) {
        return ResponseEntity.ok(messageBus.getMessages(executionId));
    }
}
