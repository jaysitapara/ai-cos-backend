package com.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code jwt.*} block of application.yml, which is sourced from the
 * JWT_SECRET / JWT_EXPIRATION environment variables.
 *
 * @param secret     HMAC signing key; must be at least 32 characters for HS256
 * @param expiration token lifetime in milliseconds
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    @NotBlank String secret,
    @Positive long expiration
) {
    /** Minimum key length (bytes) accepted by the HS256 algorithm. */
    public static final int MIN_SECRET_LENGTH = 32;
}
