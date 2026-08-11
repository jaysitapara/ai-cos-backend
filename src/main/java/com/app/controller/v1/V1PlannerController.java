package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.planner.ExecutionPlanModel;
import com.app.model.planner.TaskItem;
import com.app.service.TaskPlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/planner")
@RequiredArgsConstructor
@Tag(name = "V1 Task Planner", description = "Task Planning & Execution Engine REST APIs")
public class V1PlannerController {

    private final TaskPlannerService plannerService;

    @PostMapping("/plans")
    @Operation(summary = "Create an execution plan for a goal")
    public ResponseEntity<ExecutionPlanModel> createPlan(@RequestBody Map<String, String> body) {
        String executionIdStr = body.get("executionId");
        UUID executionId = executionIdStr != null ? UUID.fromString(executionIdStr) : UUID.randomUUID();
        String goalPrompt = body.getOrDefault("goalPrompt", "Standard Goal");

        return ResponseEntity.ok(plannerService.createPlan(executionId, goalPrompt));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get execution plan by ID")
    public ResponseEntity<ExecutionPlanModel> getPlan(@PathVariable String id) {
        return plannerService.getPlan(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get task details by ID")
    public ResponseEntity<TaskItem> getTask(@PathVariable String id) {
        return plannerService.getTask(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tasks/{id}/cancel")
    @Operation(summary = "Cancel a task by ID")
    public ResponseEntity<TaskItem> cancelTask(@PathVariable String id) {
        return ResponseEntity.ok(plannerService.cancelTask(id));
    }

    @PostMapping("/tasks/{id}/retry")
    @Operation(summary = "Retry a failed or cancelled task by ID")
    public ResponseEntity<TaskItem> retryTask(@PathVariable String id) {
        return ResponseEntity.ok(plannerService.retryTask(id));
    }
}
