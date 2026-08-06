package com.app.exception;

/** Raised when a caller exceeds the credential-endpoint rate limit. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
