package com.app.service;

import com.app.enums.ApprovalGateStatus;
import com.app.enums.ApprovalGateType;
import com.app.enums.WorkflowState;
import com.app.model.planner.ExecutionPlanModel;
import com.app.model.workflow.ApprovalGate;
import com.app.model.workflow.WorkflowModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowService {

    private final TaskPlannerService plannerService;
    private final Map<String, WorkflowModel> workflowStore = new ConcurrentHashMap<>();

    public WorkflowService(TaskPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    public WorkflowModel createWorkflow(UUID executionId, String goalPrompt) {
        String workflowId = "wf-" + UUID.randomUUID();
        ExecutionPlanModel plan = plannerService.createPlan(executionId, goalPrompt);

        ApprovalGate planGate = ApprovalGate.builder()
            .gateId("gate-" + UUID.randomUUID())
            .workflowId(workflowId)
            .type(ApprovalGateType.PLAN_APPROVAL)
            .status(ApprovalGateStatus.PENDING)
            .requestedAt(Instant.now())
            .build();

        WorkflowModel workflow = WorkflowModel.builder()
            .workflowId(workflowId)
            .executionId(executionId)
            .name("Goal Execution Workflow: " + goalPrompt)
            .description("DAG Workflow for goal execution with branching and approval gates.")
            .version("1.0.0")
            .state(WorkflowState.WAITING_APPROVAL)
            .tasks(plan.getTasks())
            .dependencies(List.of(Map.of("from", "task-1", "to", "task-2", "type", "BLOCKING")))
            .branches(List.of(Map.of("branchId", "branch-qa", "condition", "plan.hasCodeChanges == true")))
            .approvalGates(List.of(planGate))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        workflowStore.put(workflowId, workflow);
        return workflow;
    }

    public Optional<WorkflowModel> getWorkflow(String workflowId) {
        return Optional.ofNullable(workflowStore.get(workflowId));
    }

    public List<WorkflowModel> listWorkflows() {
        return new ArrayList<>(workflowStore.values());
    }

    public WorkflowModel approveGate(String workflowId, String gateId, boolean approved, String note) {
        WorkflowModel workflow = workflowStore.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        for (ApprovalGate gate : workflow.getApprovalGates()) {
            if (gate.getGateId().equals(gateId)) {
                gate.setStatus(approved ? ApprovalGateStatus.APPROVED : ApprovalGateStatus.REJECTED);
                gate.setApproverNote(note);
                gate.setDecidedAt(Instant.now());
            }
        }

        boolean allApproved = workflow.getApprovalGates().stream()
            .allMatch(g -> g.getStatus() == ApprovalGateStatus.APPROVED);

        if (allApproved) {
            workflow.setState(WorkflowState.READY);
        } else {
            workflow.setState(WorkflowState.FAILED);
        }
        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowModel pauseWorkflow(String workflowId) {
        WorkflowModel wf = workflowStore.get(workflowId);
        if (wf != null) {
            wf.setState(WorkflowState.PAUSED);
            wf.setUpdatedAt(Instant.now());
        }
        return wf;
    }

    public WorkflowModel resumeWorkflow(String workflowId) {
        WorkflowModel wf = workflowStore.get(workflowId);
        if (wf != null) {
            wf.setState(WorkflowState.RUNNING);
            wf.setUpdatedAt(Instant.now());
        }
        return wf;
    }

    public WorkflowModel cancelWorkflow(String workflowId) {
        WorkflowModel wf = workflowStore.get(workflowId);
        if (wf != null) {
            wf.setState(WorkflowState.CANCELLED);
            wf.setUpdatedAt(Instant.now());
        }
        return wf;
    }
}
