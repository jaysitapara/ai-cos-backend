package com.app.common;

import java.util.List;

/**
 * Global, compile-time constants shared across layers.
 * Declared here so that no layer has to repeat magic strings.
 */
public final class ApiConstants {

    private ApiConstants() {
        // Private constructor for constants holder
    }

    /** Mandatory version prefix for every public endpoint. */
    public static final String API_V1 = "/v1";

    public static final String USERS_PATH = API_V1 + "/users";
    public static final String AUTH_PATH = API_V1 + "/auth";
    public static final String SESSIONS_PATH = API_V1 + "/sessions";
    public static final String HEALTH_PATH = API_V1 + "/health";

    /** Actor recorded in audit columns when no authenticated principal exists. */
    public static final String SYSTEM_ACTOR = "system";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /** Endpoints reachable without a JWT bearer token. */
    public static final List<String> PUBLIC_ENDPOINTS = List.of(
        HEALTH_PATH + "/**",
        AUTH_PATH + "/register",
        AUTH_PATH + "/login",
        AUTH_PATH + "/oauth",
        AUTH_PATH + "/verify-email",
        AUTH_PATH + "/forgot-password",
        AUTH_PATH + "/reset-password",
        AUTH_PATH + "/refresh-token",
        AUTH_PATH + "/logout",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**"
    );
}
