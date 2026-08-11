package com.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AppConfigServiceTest {

    @Autowired
    private AppConfigService appConfigService;

    @Test
    void testCentralizedConfigLoading() {
        assertNotNull(appConfigService.getJwtSecret());
        assertNotNull(appConfigService.getJwtRefreshSecret());
        assertNotNull(appConfigService.getGeminiApiKey());
        assertNotNull(appConfigService.getOpenAiApiKey());
        assertNotNull(appConfigService.getDatabaseUrl());
        assertNotNull(appConfigService.getNormalizedAiMode());

        assertEquals("[NOT SET]", appConfigService.maskSecret(null));
        assertEquals("********", appConfigService.maskSecret("12345678"));
        assertTrue(appConfigService.maskSecret("test-secret-key-used-only-for-automated-tests").contains("..."));
    }

    @Test
    void testStartupValidation() {
        assertDoesNotThrow(() -> appConfigService.validateRequiredVariables(true));
    }
}
