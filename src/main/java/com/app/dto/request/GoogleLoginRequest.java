package com.app.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for "Sign in with Google". The ID token is the only field accepted —
 * identity attributes are read from the verified token, never from the client.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GoogleLoginRequest(
    @NotBlank(message = "Google ID token must not be blank")
    String idToken
) {}
