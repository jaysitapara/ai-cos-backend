package com.app.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OAuthLoginRequest(
    @NotBlank(message = "Provider must not be blank")
    String provider,

    @NotBlank(message = "Provider user ID must not be blank")
    String providerUserId,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Full name must not be blank")
    String fullName
) {}
