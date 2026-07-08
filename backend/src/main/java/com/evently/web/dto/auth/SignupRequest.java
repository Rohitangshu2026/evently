package com.evently.web.dto.auth;

import com.evently.domain.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/signup}.
 *
 * @param email    the new user's email (must be unique)
 * @param password the plaintext password (hashed server-side; never stored raw)
 * @param name     the user's display name
 * @param role     the capacity the user is registering as
 */
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String name,
        @NotNull RoleEnum role
) {
}
