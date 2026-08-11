package com.app.config;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class AppConfigService {

    private static final Logger log = LoggerFactory.getLogger(AppConfigService.class);

    @Value("${ai.mode:${AI_MODE:gemini}}")
    private String aiMode;

    @Value("${gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")
    private String openAiApiKey;

    @Value("${tavily.api-key:}")
    private String tavilyApiKey;

    @Value("${upstash.redis.rest-url:}")
    private String upstashRedisRestUrl;

    @Value("${upstash.redis.rest-token:}")
    private String upstashRedisRestToken;

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.refresh-secret:}")
    private String jwtRefreshSecret;

    @Value("${github.token:}")
    private String githubToken;

    @Value("${vercel.token:}")
    private String vercelToken;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    public String getNormalizedAiMode() {
        if (aiMode == null) {
            return "gemini";
        }
        return aiMode.trim().toLowerCase();
    }

    /**
     * Validates required environment variables during startup.
     * Throws IllegalStateException if critical required secrets are unconfigured.
     */
    public void validateRequiredVariables(boolean activeTestProfile) {
        log.info("Centralized Configuration Layer: Validating required environment variables...");

        List<String> missingVars = new ArrayList<>();

        String mode = getNormalizedAiMode();
        if (!"gemini".equals(mode) && !"openai".equals(mode)) {
            String errorMsg = String.format(
                "CRITICAL CONFIGURATION ERROR: Invalid AI_MODE value '%s'. Supported values are ONLY 'gemini' and 'openai'.",
                aiMode
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            missingVars.add("JWT_SECRET");
        }
        if (jwtRefreshSecret == null || jwtRefreshSecret.trim().isEmpty()) {
            missingVars.add("JWT_REFRESH_SECRET");
        }
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            missingVars.add("DATABASE_URL");
        }

        if (!activeTestProfile) {
            if ("gemini".equals(mode) && (geminiApiKey == null || geminiApiKey.trim().isEmpty())) {
                missingVars.add("GEMINI_API_KEY");
            }
            if ("openai".equals(mode) && (openAiApiKey == null || openAiApiKey.trim().isEmpty())) {
                missingVars.add("OPENAI_API_KEY");
            }
        }

        if (!missingVars.isEmpty() && !activeTestProfile) {
            String errorMsg = String.format(
                "CRITICAL CONFIGURATION ERROR: The following required environment variables are missing or empty: %s. " +
                "Please configure them in your .env file or server environment before starting the application.",
                missingVars
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        log.info("Centralized Configuration Layer: Active AI_MODE is '{}'. All required environment variables successfully loaded and validated.", mode);
    }

    /**
     * Utility method to mask sensitive tokens for logging purposes.
     */
    public String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "[NOT SET]";
        }
        if (secret.length() <= 8) {
            return "********";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }
}
