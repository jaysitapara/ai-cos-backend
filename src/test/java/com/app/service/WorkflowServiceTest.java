package com.app.service;

import com.app.enums.ApprovalGateStatus;
import com.app.enums.WorkflowState;
import com.app.model.workflow.WorkflowModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowServiceTest {

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        DependencyGraphResolver resolver = new DependencyGraphResolver();
        TaskPlannerService plannerService = new TaskPlannerService(resolver);
        workflowService = new WorkflowService(plannerService);
    }

    @Test
    void testWorkflowLifecycleAndApprovalGate() {
        UUID executionId = UUID.randomUUID();
        String goal = "Deploy multi-tier database cluster";

        WorkflowModel wf = workflowService.createWorkflow(executionId, goal);

        assertNotNull(wf);
        assertEquals(WorkflowState.WAITING_APPROVAL, wf.getState());
        assertEquals(1, wf.getApprovalGates().size());

        String gateId = wf.getApprovalGates().get(0).getGateId();

        // Approve Gate
        WorkflowModel approvedWf = workflowService.approveGate(wf.getWorkflowId(), gateId, true, "Approved for deployment");

        assertEquals(WorkflowState.READY, approvedWf.getState());
        assertEquals(ApprovalGateStatus.APPROVED, approvedWf.getApprovalGates().get(0).getStatus());

        // Pause & Cancel
        WorkflowModel paused = workflowService.pauseWorkflow(wf.getWorkflowId());
        assertEquals(WorkflowState.PAUSED, paused.getState());

        WorkflowModel cancelled = workflowService.cancelWorkflow(wf.getWorkflowId());
        assertEquals(WorkflowState.CANCELLED, cancelled.getState());
    }
}
