package com.app.model;

import com.app.enums.UserRole;
import com.app.enums.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDomainModel(
    Long id,
    UUID publicId,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    boolean emailVerified,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
