package com.app.service;

import com.app.enums.RuntimeState;
import com.app.model.runtime.RuntimeContextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRuntimeServiceTest {

    private ExecutionRuntimeService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionRuntimeService();
    }

    @Test
    void testStartAndManageRuntime() {
        RuntimeContextModel context = service.startRuntime("plan-777");

        assertNotNull(context);
        assertEquals("plan-777", context.getPlanId());
        assertEquals(RuntimeState.RUNNING, context.getStatus());

        RuntimeContextModel paused = service.pauseRuntime(context.getRuntimeId());
        assertEquals(RuntimeState.PAUSED, paused.getStatus());

        RuntimeContextModel resumed = service.resumeRuntime(context.getRuntimeId());
        assertEquals(RuntimeState.RUNNING, resumed.getStatus());

        RuntimeContextModel cancelled = service.cancelRuntime(context.getRuntimeId());
        assertEquals(RuntimeState.CANCELLED, cancelled.getStatus());
    }
}
