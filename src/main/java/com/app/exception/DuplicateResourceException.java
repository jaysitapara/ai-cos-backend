package com.app.exception;

/** Raised when a business uniqueness rule is violated (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
