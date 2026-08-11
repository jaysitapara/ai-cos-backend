package com.app.service;

import com.app.model.requirement.RequirementAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequirementAnalyzerTest {

    private RequirementAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new RequirementAnalyzer();
    }

    @Test
    void testAnalyzeGoalAndRequirements() {
        String goal = "Create real-time stock alert dashboard with email notification";
        RequirementAnalysisResult result = analyzer.analyzeGoal(goal, List.of("doc-1"));

        assertNotNull(result);
        assertEquals(goal, result.getGoalPrompt());
        assertEquals(2, result.getRequirements().size());
        assertTrue(result.getAssumptions().size() > 0);

        // Update assumptions
        RequirementAnalysisResult updated = analyzer.updateAssumptions(result.getAnalysisId(), List.of("New Assumption"));
        assertEquals(1, updated.getAssumptions().size());
        assertEquals("New Assumption", updated.getAssumptions().get(0));
    }
}
