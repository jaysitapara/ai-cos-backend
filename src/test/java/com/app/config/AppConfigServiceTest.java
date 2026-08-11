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
        assertNotNull(appConfigService.getGroqApiKey());
        assertNotNull(appConfigService.getTavilyApiKey());
        assertNotNull(appConfigService.getUpstashRedisRestUrl());
        assertNotNull(appConfigService.getUpstashRedisRestToken());
        assertNotNull(appConfigService.getDatabaseUrl());
        assertNotNull(appConfigService.getRedisUrl());
        assertNotNull(appConfigService.getGithubToken());
        assertNotNull(appConfigService.getVercelToken());
        assertNotNull(appConfigService.getResendApiKey());

        assertEquals("[NOT SET]", appConfigService.maskSecret(null));
        assertEquals("********", appConfigService.maskSecret("12345678"));
        assertTrue(appConfigService.maskSecret("test-secret-key-used-only-for-automated-tests").contains("..."));
    }

    @Test
    void testStartupValidation() {
        assertDoesNotThrow(() -> appConfigService.validateRequiredVariables(true));
    }
}
