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
    public static final String DASHBOARD_PATH = API_V1 + "/dashboard";
    public static final String HEALTH_PATH = API_V1 + "/health";

    /** Actor recorded in audit columns when no authenticated principal exists. */
    public static final String SYSTEM_ACTOR = "system";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String AGENT_WORKSPACE_PATH = "/api/v1/agent-workspace";

    /**
     * Endpoints reachable without a JWT bearer token.
     *
     * <p>Strictly the credential-establishing routes plus documentation. Anything
     * that reads or writes a user's own data must stay off this list — the agent
     * workspace routes were previously public, which let any unauthenticated
     * caller read and mutate other people's sessions and artifacts.
     *
     * <p>{@code /auth/logout} is public on purpose: revoking a refresh token has
     * to work even once the access token has already expired.
     */
    public static final List<String> PUBLIC_ENDPOINTS = List.of(
        HEALTH_PATH + "/**",
        AUTH_PATH + "/register",
        AUTH_PATH + "/login",
        AUTH_PATH + "/google",
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
