package com.app.service;

import com.app.model.crossproject.CrossProjectRecommendation;
import com.app.model.crossproject.ProjectRelationshipGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrossProjectIntelligenceServiceTest {

    private CrossProjectIntelligenceService service;

    @BeforeEach
    void setUp() {
        service = new CrossProjectIntelligenceService();
    }

    @Test
    void testRecommendationsAndRelationshipGraph() {
        List<CrossProjectRecommendation> recs = service.getRecommendations("proj-100");
        assertFalse(recs.isEmpty());
        assertTrue(recs.get(0).getConfidenceScore() >= 0.90);

        ProjectRelationshipGraph graph = service.getRelationshipGraph("proj-100");
        assertNotNull(graph);
        assertEquals("proj-100", graph.getProjectId());
        assertFalse(graph.getRelatedProjectIds().isEmpty());
    }
}
