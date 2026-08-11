package com.app.service;

import com.app.enums.TaskState;
import com.app.model.planner.ExecutionPlanModel;
import com.app.model.planner.TaskItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskPlannerService {

    private final DependencyGraphResolver graphResolver;
    private final Map<String, ExecutionPlanModel> planStore = new ConcurrentHashMap<>();
    private final Map<String, TaskItem> taskStore = new ConcurrentHashMap<>();

    public TaskPlannerService(DependencyGraphResolver graphResolver) {
        this.graphResolver = graphResolver;
    }

    public ExecutionPlanModel createPlan(UUID executionId, String goalPrompt) {
        String planId = "plan-" + UUID.randomUUID();

        TaskItem task1 = TaskItem.builder()
            .taskId("task-1")
            .executionId(executionId)
            .name("Goal Analysis & Requirement Breakdown")
            .description("Deconstruct user goal prompt into structured sub-tasks")
            .objective("Establish baseline scope")
            .priority(1)
            .status(TaskState.READY)
            .assignedAgentId("agent-planner-01")
            .dependencies(List.of())
            .inputs(Map.of("goalPrompt", goalPrompt))
            .outputs(Map.of())
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        TaskItem task2 = TaskItem.builder()
            .taskId("task-2")
            .executionId(executionId)
            .name("System Architecture & Verification Check")
            .description("Verify compliance and generate system architecture draft")
            .objective("Ensure safety and system standards")
            .priority(2)
            .status(TaskState.WAITING)
            .assignedAgentId("agent-qa-01")
            .dependencies(List.of("task-1"))
            .inputs(Map.of())
            .outputs(Map.of())
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        List<TaskItem> tasks = List.of(task1, task2);

        if (graphResolver.hasCircularDependency(tasks)) {
            throw new IllegalStateException("Circular dependency detected in execution plan tasks.");
        }

        tasks.forEach(t -> taskStore.put(t.getTaskId(), t));

        ExecutionPlanModel plan = ExecutionPlanModel.builder()
            .planId(planId)
            .executionId(executionId)
            .goalPrompt(goalPrompt)
            .summary("Structured 2-step execution plan for goal: " + goalPrompt)
            .tasks(tasks)
            .totalEstimatedComplexity(3)
            .createdAt(Instant.now())
            .build();

        planStore.put(planId, plan);
        return plan;
    }

    public Optional<ExecutionPlanModel> getPlan(String planId) {
        return Optional.ofNullable(planStore.get(planId));
    }

    public Optional<TaskItem> getTask(String taskId) {
        return Optional.ofNullable(taskStore.get(taskId));
    }

    public TaskItem cancelTask(String taskId) {
        TaskItem task = taskStore.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        task.setStatus(TaskState.CANCELLED);
        task.setUpdatedAt(Instant.now());
        return task;
    }

    public TaskItem retryTask(String taskId) {
        TaskItem task = taskStore.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        task.setStatus(TaskState.READY);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setUpdatedAt(Instant.now());
        return task;
    }
}
