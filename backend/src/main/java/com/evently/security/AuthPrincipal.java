package com.evently.security;

import com.evently.domain.enums.RoleEnum;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated principal placed in the security context by
 * {@link JwtAuthenticationFilter}. Derived entirely from the verified access
 * token, so no database lookup is needed per request.
 *
 * @param userId the authenticated user's id (the token {@code sub})
 * @param email  the user's email
 * @param roles  the user's roles (also reflected as {@code ROLE_*} authorities)
 */
public record AuthPrincipal(UUID userId, String email, Set<RoleEnum> roles) {
}
