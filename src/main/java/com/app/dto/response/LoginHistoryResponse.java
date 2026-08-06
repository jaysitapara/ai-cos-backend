package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginHistoryResponse(
    UUID publicId,
    String email,
    String status,
    String failureReason,
    String ipAddress,
    String userAgent,
    OffsetDateTime createdAt
) {}
