package com.evently.web.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is semantically invalid beyond what bean validation
 * catches. Maps to HTTP 400.
 */
public class BadRequestException extends ApiException {

    /** @param message client-safe description of what was wrong with the request */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
