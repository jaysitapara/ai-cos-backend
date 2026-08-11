package com.app.service;

import com.app.model.review.CodeReviewReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeReviewServiceTest {

    private CodeReviewService service;

    @BeforeEach
    void setUp() {
        service = new CodeReviewService();
    }

    @Test
    void testRunCodeReview() {
        CodeReviewReport report = service.runCodeReview("bep-100");

        assertNotNull(report);
        assertEquals("bep-100", report.getPlanId());
        assertTrue(report.getOverallQualityScore() >= 90.0);
        assertEquals(2, report.getIssues().size());
        assertFalse(report.getOptimizationSuggestions().isEmpty());
    }
}
