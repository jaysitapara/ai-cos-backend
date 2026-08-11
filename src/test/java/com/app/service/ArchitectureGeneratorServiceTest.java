package com.app.service;

import com.app.model.architecture.TechnicalBlueprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureGeneratorServiceTest {

    private ArchitectureGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new ArchitectureGeneratorService();
    }

    @Test
    void testGenerateBlueprint() {
        TechnicalBlueprint blueprint = service.generateBlueprint("spec-123");

        assertNotNull(blueprint);
        assertEquals("spec-123", blueprint.getSpecId());
        assertEquals(1, blueprint.getEntities().size());
        assertEquals(1, blueprint.getApiEndpoints().size());
        assertTrue(blueprint.getSystemArchitectureMarkdown().contains("Clean Architecture"));
    }
}
