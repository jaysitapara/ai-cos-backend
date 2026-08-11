package com.app.service;

import com.app.model.backendgen.BackendPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackendGeneratorServiceTest {

    private BackendGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new BackendGeneratorService();
    }

    @Test
    void testGenerateBackendPlan() {
        BackendPlan plan = service.generateBackendPlan("blue-123");

        assertNotNull(plan);
        assertEquals("blue-123", plan.getBlueprintId());
        assertTrue(plan.getFeatureModules().contains("com.app.controller.v1"));
        assertNotNull(plan.getDocumentationArtifacts().get("README.md"));
        assertNotNull(plan.getTestingStrategy().get("unitTestFramework"));
    }
}
