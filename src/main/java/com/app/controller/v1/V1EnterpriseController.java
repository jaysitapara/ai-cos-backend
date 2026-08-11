package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.enterprise.EnterprisePolicy;
import com.app.model.enterprise.OrganizationModel;
import com.app.service.EnterpriseGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/enterprise")
@RequiredArgsConstructor
@Tag(name = "V1 Enterprise Governance Engine", description = "Enterprise Readiness, Multi-Tenancy & Compliance REST APIs")
public class V1EnterpriseController {

    private final EnterpriseGovernanceService enterpriseGovernanceService;

    @GetMapping("/organization")
    @Operation(summary = "Get enterprise organization governance details")
    public ResponseEntity<OrganizationModel> getOrganizationDetails() {
        return ResponseEntity.ok(enterpriseGovernanceService.getOrganizationDetails());
    }

    @GetMapping("/policies")
    @Operation(summary = "List enterprise security and RBAC governance policies")
    public ResponseEntity<EnterprisePolicy> getPolicy() {
        return ResponseEntity.ok(enterpriseGovernanceService.getEnterprisePolicy());
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get enterprise compliance verification status (SOC2, GDPR, ISO 27001)")
    public ResponseEntity<Map<String, Object>> getComplianceStatus() {
        return ResponseEntity.ok(enterpriseGovernanceService.getComplianceStatus());
    }
}
