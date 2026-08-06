package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProjectResponse(
    UUID publicId,
    String name,
    String description,
    String status,
    int progress,
    OffsetDateTime dueDate,
    OffsetDateTime updatedAt
) {}
