package com.app.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChangePasswordRequest(
    @NotBlank(message = "Current password must not be blank")
    String currentPassword,

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
    String newPassword
) {}
