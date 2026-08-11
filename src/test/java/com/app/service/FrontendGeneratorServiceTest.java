package com.app.service;

import com.app.model.frontend.FrontendPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontendGeneratorServiceTest {

    private FrontendGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new FrontendGeneratorService();
    }

    @Test
    void testGenerateFrontendPlan() {
        FrontendPlan plan = service.generateFrontendPlan("blue-123");

        assertNotNull(plan);
        assertEquals("blue-123", plan.getBlueprintId());
        assertTrue(plan.getRouteStructure().contains("/app/workspace"));
        assertEquals(2, plan.getComponents().size());
        assertNotNull(plan.getDesignSystemTokens().get("primaryColor"));
    }
}
