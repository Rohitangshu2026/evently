package com.evently.repo;

import com.evently.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link RefreshToken}. Supports the rotation and
 * reuse-detection flow: look up a presented token by its hash, and revoke a
 * whole token family when reuse is detected.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Finds a token by the SHA-256 hash of its raw value. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** All tokens in a rotation family (used when revoking on reuse). */
    List<RefreshToken> findByFamily(UUID family);

    /** Only the still-active tokens in a family. */
    List<RefreshToken> findByFamilyAndRevokedAtIsNull(UUID family);
}
