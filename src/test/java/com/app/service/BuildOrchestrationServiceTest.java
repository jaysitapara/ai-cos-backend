package com.app.service;

import com.app.enums.BuildStatus;
import com.app.model.build.BuildJobModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildOrchestrationServiceTest {

    private BuildOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new BuildOrchestrationService();
    }

    @Test
    void testCreateBuildAndArtifacts() {
        BuildJobModel build = service.createBuild("repo-100", "main");

        assertNotNull(build);
        assertEquals("repo-100", build.getRepositoryId());
        assertEquals(BuildStatus.SUCCESS, build.getStatus());
        assertTrue(build.getPipelineYaml().contains("AI-COS Autonomous CI/CD Pipeline"));
        assertEquals(2, build.getArtifacts().size());
    }
}
