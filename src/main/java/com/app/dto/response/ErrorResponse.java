package com.app.dto.response;

import com.app.enums.ErrorCode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/** Single error envelope used by every non-2xx response (see API Standard). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErrorResponse(
    ErrorCode errorCode,
    String errorMessage,
    List<FieldErrorItem> fieldErrors
) {

    public static ErrorResponse of(ErrorCode errorCode, String errorMessage) {
        return new ErrorResponse(errorCode, errorMessage, List.of());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FieldErrorItem(
        String field,
        String message
    ) {}
}
