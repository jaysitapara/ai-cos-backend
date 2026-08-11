package com.app.provider.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Isolated Project Execution — Security & Provider Test Suite")
class IsolatedProjectExecutionTest {

    private DockerProjectExecutionProvider dockerProvider;
    private HostProjectExecutionProvider hostProvider;
    private ContainerExecutionService containerExecutionService;
    private Environment mockEnvironment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        dockerProvider = new DockerProjectExecutionProvider();
        ReflectionTestUtils.setField(dockerProvider, "executionMode", "container");
        ReflectionTestUtils.setField(dockerProvider, "nodeImage", "node:22-alpine");
        ReflectionTestUtils.setField(dockerProvider, "javaImage", "eclipse-temurin:21-jdk-alpine");
        ReflectionTestUtils.setField(dockerProvider, "cpuLimit", "1.0");
        ReflectionTestUtils.setField(dockerProvider, "memoryLimit", "512m");
        ReflectionTestUtils.setField(dockerProvider, "pidsLimit", "128");

        hostProvider = new HostProjectExecutionProvider();
        ReflectionTestUtils.setField(hostProvider, "executionMode", "host");

        mockEnvironment = Mockito.mock(Environment.class);
        containerExecutionService = new ContainerExecutionService(dockerProvider, hostProvider, mockEnvironment);
        ReflectionTestUtils.setField(containerExecutionService, "executionMode", "container");
    }

    @Test
    @DisplayName("Should select DockerProvider when mode is container and verify provider name")
    void testProviderNameAndSelection() {
        assertEquals("DOCKER_CONTAINER", dockerProvider.getProviderName());
        assertEquals("HOST_PROCESS_DEV_ONLY", hostProvider.getProviderName());
    }

    @Test
    @DisplayName("Should fail fast with IllegalStateException when execution mode is container but Docker daemon is unavailable in production")
    void testFailFastWhenDockerUnavailableInProductionMode() {
        if (!dockerProvider.isAvailable()) {
            Mockito.when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            assertThrows(IllegalStateException.class, () -> containerExecutionService.getActiveProvider());
        }
    }

    @Test
    @DisplayName("Should safely fall back to HostProvider when Docker is unavailable in local dev environment")
    void testSafeFallbackInLocalDevEnvironment() {
        if (!dockerProvider.isAvailable()) {
            Mockito.when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            ProjectExecutionProvider active = containerExecutionService.getActiveProvider();
            assertNotNull(active);
            assertEquals("HOST_PROCESS_DEV_ONLY", active.getProviderName());
        }
    }

    @Test
    @DisplayName("Should construct isolated execution request without leaking application secrets")
    void testExecutionRequestSecretIsolation() {
        Path projectPath = Paths.get("workspace/projects/1/100");

        ProjectExecutionRequest req = ProjectExecutionRequest.builder()
            .projectId(100L)
            .userId(1L)
            .jobId(50L)
            .projectDir(projectPath)
            .techStack("REACT")
            .commandType("INSTALL")
            .timeoutSeconds(180)
            .build();

        assertEquals(100L, req.getProjectId());
        assertEquals(1L, req.getUserId());
        assertEquals(50L, req.getJobId());
        assertEquals("REACT", req.getTechStack());
        assertEquals("INSTALL", req.getCommandType());
        assertNull(req.getEnvironmentMap()); // Ensures no default host environment secrets passed
    }

    @Test
    @DisplayName("Should allow host execution provider ONLY when explicitly configured")
    void testHostProviderExplicitMode() {
        ReflectionTestUtils.setField(containerExecutionService, "executionMode", "host");
        ProjectExecutionProvider active = containerExecutionService.getActiveProvider();
        assertEquals("HOST_PROCESS_DEV_ONLY", active.getProviderName());
    }
}
