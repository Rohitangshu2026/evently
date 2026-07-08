package com.evently.web.dto.auth;

/**
 * Response body for successful signup/login/refresh. The refresh token itself is
 * never in the body — it is delivered as an httpOnly cookie.
 *
 * @param accessToken the signed JWT access token
 * @param tokenType   always {@code "Bearer"}
 * @param expiresIn   access-token lifetime in seconds
 * @param user        the authenticated user
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
