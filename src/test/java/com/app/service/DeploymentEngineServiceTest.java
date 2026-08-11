package com.app.service;

import com.app.enums.DeploymentStrategy;
import com.app.enums.TargetEnvironment;
import com.app.model.deployment.DeploymentJobModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentEngineServiceTest {

    private DeploymentEngineService service;

    @BeforeEach
    void setUp() {
        service = new DeploymentEngineService();
    }

    @Test
    void testCreateAndPromoteDeployment() {
        DeploymentJobModel dep = service.createDeployment("bld-123", TargetEnvironment.STAGING, DeploymentStrategy.BLUE_GREEN);

        assertNotNull(dep);
        assertEquals(TargetEnvironment.STAGING, dep.getEnvironment());
        assertEquals("SUCCESS", dep.getStatus());
        assertTrue(dep.getLiveUrl().contains("staging"));

        // Promote to PRODUCTION
        DeploymentJobModel promoted = service.promoteEnvironment(dep.getDeploymentId(), TargetEnvironment.PRODUCTION);
        assertEquals(TargetEnvironment.PRODUCTION, promoted.getEnvironment());
        assertTrue(promoted.getLiveUrl().contains("production"));
    }
}
