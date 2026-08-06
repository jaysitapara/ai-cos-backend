package com.app.dto.response;

import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserResponse(
    UUID publicId,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    boolean emailVerified,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {}
