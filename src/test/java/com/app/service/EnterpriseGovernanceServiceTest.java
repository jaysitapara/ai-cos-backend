package com.app.service;

import com.app.model.enterprise.EnterprisePolicy;
import com.app.model.enterprise.OrganizationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnterpriseGovernanceServiceTest {

    private EnterpriseGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new EnterpriseGovernanceService();
    }

    @Test
    void testOrganizationPolicyAndCompliance() {
        OrganizationModel org = service.getOrganizationDetails();
        assertNotNull(org);
        assertEquals("ENTERPRISE", org.getPlanTier());
        assertTrue(org.getComplianceFrameworks().contains("SOC2_TYPE_II"));

        EnterprisePolicy policy = service.getEnterprisePolicy();
        assertNotNull(policy);
        assertTrue(policy.isRbacEnforced());

        Map<String, Object> compliance = service.getComplianceStatus();
        assertNotNull(compliance);
        assertEquals("PASSED", compliance.get("soc2Audit"));
    }
}
