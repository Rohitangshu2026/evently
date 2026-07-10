package com.evently.web.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist (or is not visible to the
 * caller). Maps to HTTP 404.
 */
public class ResourceNotFoundException extends ApiException {

    /** @param message client-safe description of what was not found */
    public ResourceNotFoundException(String message){
        super(HttpStatus.NOT_FOUND, message);
    }
}
