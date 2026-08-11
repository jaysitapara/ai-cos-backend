package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.workflow.WorkflowModel;
import com.app.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/workflows")
@RequiredArgsConstructor
@Tag(name = "V1 Workflows", description = "Workflow Engine & Approval Checkpoint REST APIs")
public class V1WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @Operation(summary = "Create workflow definition for goal execution")
    public ResponseEntity<WorkflowModel> createWorkflow(@RequestBody Map<String, String> body) {
        String executionIdStr = body.get("executionId");
        UUID executionId = executionIdStr != null ? UUID.fromString(executionIdStr) : UUID.randomUUID();
        String goalPrompt = body.getOrDefault("goalPrompt", "Standard Workflow Goal");

        return ResponseEntity.ok(workflowService.createWorkflow(executionId, goalPrompt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow state and approval status")
    public ResponseEntity<WorkflowModel> getWorkflow(@PathVariable String id) {
        return workflowService.getWorkflow(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all workflows")
    public ResponseEntity<List<WorkflowModel>> listWorkflows() {
        return ResponseEntity.ok(workflowService.listWorkflows());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve or reject a workflow gate checkpoint")
    public ResponseEntity<WorkflowModel> approveGate(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String gateId = (String) body.get("gateId");
        Boolean approved = (Boolean) body.getOrDefault("approved", true);
        String note = (String) body.getOrDefault("note", "Approved by user");

        return ResponseEntity.ok(workflowService.approveGate(id, gateId, approved, note));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause running workflow")
    public ResponseEntity<WorkflowModel> pauseWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.pauseWorkflow(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume paused workflow")
    public ResponseEntity<WorkflowModel> resumeWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.resumeWorkflow(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel workflow execution")
    public ResponseEntity<WorkflowModel> cancelWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.cancelWorkflow(id));
    }
}
