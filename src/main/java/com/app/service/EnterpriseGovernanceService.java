package com.app.service;

import com.app.model.enterprise.EnterprisePolicy;
import com.app.model.enterprise.OrganizationModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class EnterpriseGovernanceService {

    public OrganizationModel getOrganizationDetails() {
        return OrganizationModel.builder()
            .orgId("org-enterprise-root")
            .name("AI-COS Enterprise Global")
            .planTier("ENTERPRISE")
            .teamsCount(12)
            .complianceFrameworks(List.of("SOC2_TYPE_II", "GDPR", "ISO_27001"))
            .status("ACTIVE")
            .createdAt(Instant.now())
            .build();
    }

    public EnterprisePolicy getEnterprisePolicy() {
        return EnterprisePolicy.builder()
            .policyId("pol-root")
            .orgId("org-enterprise-root")
            .aiModelRestrictions(List.of("ALLOW_APPROVED_MODELS_ONLY"))
            .dataRetentionDays(365)
            .rbacEnforced(true)
            .auditLoggingStrict(true)
            .build();
    }

    public Map<String, Object> getComplianceStatus() {
        return Map.of(
            "soc2Audit", "PASSED",
            "gdprConsentManagement", "ENFORCED",
            "iso27001Certification", "ACTIVE",
            "multiRegionDR", "SYNCED"
        );
    }
}
