package com.app.service;

import com.app.enums.HealthLevel;
import com.app.model.monitoring.SystemHealthReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryEngineServiceTest {

    private RecoveryEngineService recoveryService;

    @BeforeEach
    void setUp() {
        ContextManager contextManager = new ContextManager();
        EventDispatcher eventDispatcher = new EventDispatcher();
        ExecutionCoordinator coordinator = new ExecutionCoordinator(contextManager, eventDispatcher);
        OrchestratorService orchestratorService = new OrchestratorService(contextManager, coordinator, eventDispatcher);
        recoveryService = new RecoveryEngineService(orchestratorService);
    }

    @Test
    void testGenerateHealthReport() {
        SystemHealthReport report = recoveryService.generateHealthReport();

        assertNotNull(report);
        assertEquals(HealthLevel.HEALTHY, report.getOverallStatus());
        assertTrue(report.getSuccessRatePercent() > 90.0);
    }
}
