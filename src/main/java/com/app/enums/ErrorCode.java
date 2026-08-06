package com.app.enums;

/**
 * Stable machine-readable codes returned in the {@code error_code} field of
 * every error response, as required by the API Standard.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    UNAUTHORIZED,
    ACCESS_DENIED,
    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE,
    TOO_MANY_REQUESTS,
    INTERNAL_SERVER_ERROR
}
