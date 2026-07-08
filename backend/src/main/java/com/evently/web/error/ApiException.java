package com.evently.web.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for expected, client-facing errors. Each subclass carries the HTTP
 * status the {@link GlobalExceptionHandler} should return; the message becomes
 * the {@link ErrorResponse#error()} body.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * @param status  HTTP status to respond with
     * @param message client-safe error message
     */
    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** @return the HTTP status this exception maps to */
    public HttpStatus getStatus() {
        return status;
    }
}
