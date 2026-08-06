package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SessionResponse(
    UUID publicId,
    String deviceName,
    String deviceType,
    String ipAddress,
    String userAgent,
    String location,
    OffsetDateTime lastAccessedAt,
    OffsetDateTime expiresAt,
    boolean isCurrent
) {}
