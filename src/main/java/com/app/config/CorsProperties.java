package com.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code app.cors.*}. Allows the Vite dev server (and any deployed
 * frontend origin) to call the API directly instead of through a proxy.
 *
 * @param allowedOrigins browser origins permitted to call the API
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
