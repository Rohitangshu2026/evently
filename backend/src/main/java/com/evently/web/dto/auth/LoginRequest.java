package com.evently.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/login}.
 *
 * @param email    the user's email
 * @param password the user's plaintext password
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
