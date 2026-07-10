package com.evently.web.error;

/**
 * The single JSON error shape returned by every failing endpoint:
 * {@code { "error": "..." }}. Mirrors the frontend's {@code ErrorResponse} type.
 *
 * @param error a human-readable, client-safe error message
 */
public record ErrorResponse(String error){
}
