package com.evently.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions thrown by controllers/services into the uniform
 * {@link ErrorResponse} shape, so clients always receive {@code { "error": ... }}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all expected, client-facing errors using the status carried by
     * the {@link ApiException}.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles bean-validation failures on {@code @Valid} request bodies,
     * surfacing the first field error as the message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("The request was malformed.");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /** Handles authorization failures for authenticated-but-forbidden callers (403). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("You don't have permission to perform that action."));
    }

    /** Catch-all for unexpected errors; logs the detail and returns a generic 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("The server ran into a problem. Please try again."));
    }
}
