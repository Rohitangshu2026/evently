package com.evently.security;

import com.evently.AbstractIntegrationTest;
import com.evently.domain.User;
import com.evently.domain.enums.RoleEnum;
import com.evently.repo.RefreshTokenRepository;
import com.evently.repo.UserRepository;
import com.evently.web.error.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the refresh-token rotation + reuse-detection contract against a real
 * PostgreSQL database: rotating a token invalidates the old one, and replaying
 * an already-rotated (revoked) token revokes the <em>entire</em> family so the
 * legitimate current token is also killed.
 */
class RefreshTokenReuseDetectionIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void reuseOfRotatedTokenRevokesEntireFamily(){
        User user = createUser();

        // Login issues token A in a fresh family.
        String rawA = refreshTokenService.issue(user, "junit", "127.0.0.1");
        UUID family = refreshTokenRepository.findByTokenHash(sha256Hex(rawA)).orElseThrow().getFamily();

        // A legitimate refresh rotates A -> B; A is now revoked.
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawA, "junit", "127.0.0.1");
        String rawB = rotation.rawToken();
        assertThat(refreshTokenRepository.findByFamilyAndRevokedAtIsNull(family)).hasSize(1);

        // Replaying the old token A must be rejected (reuse detected)...
        assertThatThrownBy(() -> refreshTokenService.rotate(rawA, "attacker", "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);

        // ...and must revoke the whole family, including the still-current token B.
        assertThat(refreshTokenRepository.findByFamilyAndRevokedAtIsNull(family)).isEmpty();
        assertThatThrownBy(() -> refreshTokenService.rotate(rawB, "junit", "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    private User createUser(){
        User user = new User();
        user.setEmail("reuse-" + UUID.randomUUID() + "@evently.test");
        user.setName("Reuse Test");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles(EnumSet.of(RoleEnum.ATTENDEE));
        return userRepository.save(user);
    }

    /** Mirrors {@link RefreshTokenService}'s token hashing so we can look tokens up. */
    private String sha256Hex(String raw){
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch(Exception e){
            throw new IllegalStateException(e);
        }
    }
}
