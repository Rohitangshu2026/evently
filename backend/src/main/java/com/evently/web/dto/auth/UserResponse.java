package com.evently.web.dto.auth;

import com.evently.domain.User;
import com.evently.domain.enums.RoleEnum;

import java.util.Set;
import java.util.UUID;

/**
 * Public view of a user, returned by the auth endpoints and {@code /auth/me}.
 * Never includes the password hash.
 *
 * @param id    the user's id
 * @param email the user's email
 * @param name  the user's display name
 * @param roles the user's roles
 */
public record UserResponse(
        UUID id,
        String email,
        String name,
        Set<RoleEnum> roles
) {
    /**
     * Maps a {@link User} entity to its public view.
     *
     * @param user the entity
     * @return the response DTO
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }
}
