package com.app.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.auth.*} block. Every value that governs lockout,
 * token lifetime or throttling lives here so the policy can be tuned per
 * environment without a redeploy.
 *
 * @param maxFailedLoginAttempts   failures before an account is temporarily locked
 * @param lockDurationMinutes      how long a locked account stays locked
 * @param refreshTokenValidityDays lifetime of an issued refresh token
 * @param verificationTokenHours   lifetime of an email verification token
 * @param passwordResetTokenHours  lifetime of a password reset token
 * @param loginHistoryRetentionDays how long login audit rows are kept
 * @param rateLimitMaxAttempts     credential attempts allowed per IP inside the window
 * @param rateLimitWindowSeconds   length of the throttling window
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    @Positive int maxFailedLoginAttempts,
    @Positive long lockDurationMinutes,
    @Positive long refreshTokenValidityDays,
    @Positive long verificationTokenHours,
    @Positive long passwordResetTokenHours,
    @Positive int loginHistoryRetentionDays,
    @Positive int rateLimitMaxAttempts,
    @Positive long rateLimitWindowSeconds
) {
}
