package com.app.service;

import com.app.model.aios.AiCapability;
import com.app.model.aios.AiOsSystemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiOperatingSystemServiceTest {

    private AiOperatingSystemService service;

    @BeforeEach
    void setUp() {
        service = new AiOperatingSystemService();
    }

    @Test
    void testSystemStatusAndCapabilities() {
        AiOsSystemStatus status = service.getSystemStatus();
        assertNotNull(status);
        assertEquals("OPERATIONAL", status.getStatus());
        assertEquals("v5.0.0-AI-OS", status.getVersion());

        List<AiCapability> capabilities = service.getCapabilities();
        assertFalse(capabilities.isEmpty());
        assertEquals(5, capabilities.size());

        Map<String, Object> config = service.getSystemConfig();
        assertNotNull(config);
        assertEquals("AI_NATIVE", config.get("osMode"));
    }
}
