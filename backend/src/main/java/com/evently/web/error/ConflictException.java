package com.evently.web.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with current state (e.g. duplicate email, or
 * a sold-out ticket type). Maps to HTTP 409.
 */
public class ConflictException extends ApiException {

    /** @param message client-safe description of the conflict */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
