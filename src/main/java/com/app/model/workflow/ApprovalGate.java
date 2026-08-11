package com.app.model.workflow;

import com.app.enums.ApprovalGateStatus;
import com.app.enums.ApprovalGateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalGate {
    private String gateId;
    private String workflowId;
    private ApprovalGateType type;
    private ApprovalGateStatus status;
    private String approverNote;
    private Instant requestedAt;
    private Instant decidedAt;
}
