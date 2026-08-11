package com.app.service;

import com.app.enums.TaskState;
import com.app.model.planner.ExecutionPlanModel;
import com.app.model.planner.TaskItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskPlannerServiceTest {

    private TaskPlannerService plannerService;
    private DependencyGraphResolver graphResolver;

    @BeforeEach
    void setUp() {
        graphResolver = new DependencyGraphResolver();
        plannerService = new TaskPlannerService(graphResolver);
    }

    @Test
    void testCreatePlanAndManageTasks() {
        UUID executionId = UUID.randomUUID();
        String goal = "Build supplier contract renewal alert system";

        ExecutionPlanModel plan = plannerService.createPlan(executionId, goal);

        assertNotNull(plan);
        assertEquals(2, plan.getTasks().size());
        assertFalse(graphResolver.hasCircularDependency(plan.getTasks()));

        // Cancel and Retry
        TaskItem cancelled = plannerService.cancelTask("task-1");
        assertEquals(TaskState.CANCELLED, cancelled.getStatus());

        TaskItem retried = plannerService.retryTask("task-1");
        assertEquals(TaskState.READY, retried.getStatus());
        assertEquals(1, retried.getRetryCount());
    }

    @Test
    void testCircularDependencyDetection() {
        TaskItem t1 = TaskItem.builder().taskId("t1").dependencies(List.of("t2")).build();
        TaskItem t2 = TaskItem.builder().taskId("t2").dependencies(List.of("t1")).build();

        assertTrue(graphResolver.hasCircularDependency(List.of(t1, t2)));
    }
}
