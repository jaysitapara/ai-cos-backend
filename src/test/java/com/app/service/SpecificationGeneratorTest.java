package com.app.service;

import com.app.model.specification.SpecificationBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecificationGeneratorTest {

    private SpecificationGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SpecificationGenerator();
    }

    @Test
    void testGenerateBrdPrdAndUserStories() {
        SpecificationBundle bundle = generator.generateSpecifications("req-999");

        assertNotNull(bundle);
        assertEquals("req-999", bundle.getAnalysisId());
        assertTrue(bundle.getBrdContent().contains("Business Requirements Document"));
        assertTrue(bundle.getPrdContent().contains("Product Requirements Document"));
        assertEquals(1, bundle.getUserStories().size());
        assertEquals("US-101", bundle.getUserStories().get(0).getStoryId());
    }
}
