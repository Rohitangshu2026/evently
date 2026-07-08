package com.evently.web.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication fails or credentials/tokens are invalid. Messages
 * are kept generic to avoid leaking whether an account exists. Maps to HTTP 401.
 */
public class UnauthorizedException extends ApiException {

    /** @param message client-safe (non-revealing) error message */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
