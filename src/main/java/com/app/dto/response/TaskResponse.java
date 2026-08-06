package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TaskResponse(
    UUID publicId,
    String title,
    String status,
    String priority,
    String projectName,
    String assigneeName,
    OffsetDateTime dueDate
) {}
