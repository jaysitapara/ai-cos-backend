package com.app.service;

import com.app.model.operations.OperationalGovernanceStatus;
import com.app.model.operations.SecuritySummaryModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductionOperationsServiceTest {

    private ProductionOperationsService service;

    @BeforeEach
    void setUp() {
        service = new ProductionOperationsService();
    }

    @Test
    void testGovernanceSecurityAndCompliance() {
        OperationalGovernanceStatus status = service.getGovernanceStatus();
        assertNotNull(status);
        assertEquals("READY", status.getDisasterRecoveryStatus());
        assertTrue(status.getComplianceScore() >= 90.0);

        SecuritySummaryModel security = service.getSecuritySummary();
        assertNotNull(security);
        assertTrue(security.isEncryptionAtRest());
        assertTrue(security.isEncryptionInTransit());

        Map<String, Object> compliance = service.getComplianceReport();
        assertNotNull(compliance);
        assertEquals("ACTIVE", compliance.get("auditLoggingStatus"));
    }
}
