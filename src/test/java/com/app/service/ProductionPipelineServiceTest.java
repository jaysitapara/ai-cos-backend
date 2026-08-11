package com.app.service;

import com.app.model.pipeline.ReleaseValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductionPipelineServiceTest {

    private ProductionPipelineService service;

    @BeforeEach
    void setUp() {
        service = new ProductionPipelineService();
    }

    @Test
    void testValidateRelease() {
        ReleaseValidationReport report = service.validateRelease("spec-100", "blue-100");

        assertNotNull(report);
        assertEquals("spec-100", report.getSpecId());
        assertTrue(report.isApproved());
        assertTrue(report.getProductionReadinessScore() >= 90.0);
        assertEquals(3, report.getQualityGates().size());
    }
}
