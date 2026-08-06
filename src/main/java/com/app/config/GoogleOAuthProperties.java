package com.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code app.oauth.google.*} block.
 *
 * <p>{@code clientId} is intentionally allowed to be blank: the application must
 * start without Google credentials configured (local development, CI), and the
 * Google endpoint reports itself as unavailable instead of failing at boot.
 *
 * @param clientId  Google OAuth client ID; ID tokens must carry it as their audience
 * @param jwkSetUri Google's public signing keys, polled and cached by the decoder
 * @param issuers   accepted {@code iss} claim values
 */
@ConfigurationProperties(prefix = "app.oauth.google")
public record GoogleOAuthProperties(
    String clientId,
    String jwkSetUri,
    List<String> issuers
) {
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }
}
