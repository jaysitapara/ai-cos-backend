package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.operations.OperationalGovernanceStatus;
import com.app.model.operations.SecuritySummaryModel;
import com.app.service.ProductionOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/production-operations")
@RequiredArgsConstructor
@Tag(name = "V1 Production Operations Engine", description = "Operations, Security & Governance REST APIs")
public class V1ProductionOperationsController {

    private final ProductionOperationsService operationsService;

    @GetMapping("/status")
    @Operation(summary = "Get overall operational governance status")
    public ResponseEntity<OperationalGovernanceStatus> getStatus() {
        return ResponseEntity.ok(operationsService.getGovernanceStatus());
    }

    @GetMapping("/security")
    @Operation(summary = "Get platform runtime security summary")
    public ResponseEntity<SecuritySummaryModel> getSecuritySummary() {
        return ResponseEntity.ok(operationsService.getSecuritySummary());
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get automated compliance report")
    public ResponseEntity<Map<String, Object>> getCompliance() {
        return ResponseEntity.ok(operationsService.getComplianceReport());
    }
}
