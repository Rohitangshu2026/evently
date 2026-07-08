package com.evently.service.auth;

import com.evently.domain.User;

/**
 * Outcome of an authentication operation, carrying everything the controller
 * needs to build the HTTP response (access token in the body, refresh token in
 * a cookie).
 *
 * @param accessToken the signed JWT access token
 * @param expiresIn   access-token lifetime in seconds
 * @param refreshToken the raw refresh token (to be set as an httpOnly cookie)
 * @param user        the authenticated user
 */
public record AuthResult(
        String accessToken,
        long expiresIn,
        String refreshToken,
        User user
) {
}
