package com.app.service;

import com.app.model.learning.LearningMetricsSummary;
import com.app.model.learning.UserFeedbackModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelfLearningServiceTest {

    private SelfLearningService service;

    @BeforeEach
    void setUp() {
        service = new SelfLearningService();
    }

    @Test
    void testFeedbackAndMetrics() {
        UserFeedbackModel fb = service.submitFeedback("user-1", "proj-1", "CODE", 5, "Excellent performance!");
        assertNotNull(fb);
        assertEquals(5, fb.getRating());
        assertEquals("PROCESSED", fb.getStatus());

        LearningMetricsSummary metrics = service.getLearningMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getRecommendationAcceptanceRate() >= 90.0);

        List<String> patterns = service.getExtractedPatterns();
        assertFalse(patterns.isEmpty());
    }
}
