package com.app.util;

import com.app.dto.response.UserResponse;
import com.app.entity.UserEntity;
import com.app.model.UserDomainModel;

/**
 * Maps persistence entities to the internal domain model and outbound DTOs.
 * Entities are never exposed directly by the API.
 */
public final class UserMapper {

    private UserMapper() {
        // Private constructor for utility class
    }

    public static UserDomainModel toDomain(UserEntity entity) {
        return new UserDomainModel(
            entity.getId(),
            entity.getPublicId(),
            entity.getFullName(),
            entity.getEmail(),
            entity.getRole(),
            entity.getStatus(),
            entity.isEmailVerified(),
            entity.getLastLoginAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static UserResponse toResponse(UserDomainModel model) {
        return new UserResponse(
            model.publicId(),
            model.fullName(),
            model.email(),
            model.role(),
            model.status(),
            model.emailVerified(),
            model.lastLoginAt(),
            model.createdAt()
        );
    }

    public static UserResponse toResponse(UserEntity entity) {
        return toResponse(toDomain(entity));
    }
}
