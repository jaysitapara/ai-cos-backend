package com.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class EnvironmentConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentConfigValidator.class);

    private final AppConfigService appConfigService;
    private final Environment environment;

    @PostConstruct
    public void validateEnvironment() {
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        log.info("Startup Environment Validator initializing (isTestProfile={})...", isTestProfile);

        // Perform strict validation via Centralized AppConfigService
        appConfigService.validateRequiredVariables(isTestProfile);

        log.info("Environment configuration validated cleanly. AI_MODE: {}, Gemini Key: {}, OpenAI Key: {}, Database: {}, Redis: {}, GitHub: {}, Vercel: {}, Resend: {}",
            appConfigService.getNormalizedAiMode(),
            setOrNotSet(appConfigService.getGeminiApiKey()),
            setOrNotSet(appConfigService.getOpenAiApiKey()),
            setOrNotSet(appConfigService.getDatabaseUrl()),
            setOrNotSet(appConfigService.getRedisUrl()),
            setOrNotSet(appConfigService.getGithubToken()),
            setOrNotSet(appConfigService.getVercelToken()),
            setOrNotSet(appConfigService.getResendApiKey())
        );
    }

    /** Returns [SET] or [NOT SET] without exposing any secret fragment. */
    private static String setOrNotSet(String value) {
        return (value != null && !value.trim().isEmpty()) ? "[SET]" : "[NOT SET]";
    }
}
