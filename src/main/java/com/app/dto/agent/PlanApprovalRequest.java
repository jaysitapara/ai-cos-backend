package com.app.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanApprovalRequest {

    @NotBlank(message = "Action must be APPROVED, REJECTED, or REQUEST_CHANGES")
    private String action; // APPROVED, REJECTED, REQUEST_CHANGES

    private String feedback;
}
