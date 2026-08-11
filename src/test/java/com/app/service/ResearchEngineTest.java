package com.app.service;

import com.app.model.research.ResearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResearchEngineTest {

    private ResearchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ResearchEngine();
    }

    @Test
    void testExecuteResearchAndRisks() {
        ResearchResult result = engine.executeResearch("req-123");

        assertNotNull(result);
        assertEquals("req-123", result.getAnalysisId());
        assertEquals("ENTERPRISE_WEB_PLATFORM", result.getDomainClassification());
        assertEquals(2, result.getRiskAnalysis().size());
        assertTrue(result.getEstimatedEffortDays() > 0);
    }
}
