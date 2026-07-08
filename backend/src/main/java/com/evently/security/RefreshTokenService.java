package com.evently.security;

import com.evently.config.JwtProperties;
import com.evently.domain.RefreshToken;
import com.evently.domain.User;
import com.evently.repo.RefreshTokenRepository;
import com.evently.web.error.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages refresh tokens with rotation and reuse detection.
 * <p>
 * Only the SHA-256 hash of each raw token is stored — the raw value lives solely
 * in the client's cookie. Every refresh <em>rotates</em> the token: the old row
 * is revoked and a new one issued in the same {@code family}. If an
 * already-revoked token is presented again (a hallmark of a stolen token being
 * replayed), the entire family is revoked, forcing a fresh login.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final RefreshTokenRevoker revoker;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @param repository    persistence for refresh tokens
     * @param revoker       commits family revocations in an independent transaction
     * @param jwtProperties supplies the refresh-token lifetime
     */
    public RefreshTokenService(RefreshTokenRepository repository,
                               RefreshTokenRevoker revoker,
                               JwtProperties jwtProperties) {
        this.repository = repository;
        this.revoker = revoker;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Issues the first refresh token of a new family (used at login/signup).
     *
     * @param user      the owner
     * @param userAgent originating user-agent (for audit), may be {@code null}
     * @param ip        originating IP (for audit), may be {@code null}
     * @return the raw refresh token to hand to the client
     */
    @Transactional
    public String issue(User user, String userAgent, String ip) {
        return persist(user, UUID.randomUUID(), null, userAgent, ip);
    }

    /**
     * Validates and rotates a presented refresh token.
     *
     * @param rawToken  the raw token from the client's cookie
     * @param userAgent originating user-agent (for audit), may be {@code null}
     * @param ip        originating IP (for audit), may be {@code null}
     * @return the owning user and a freshly issued raw token
     * @throws UnauthorizedException if the token is unknown, expired, or reused
     */
    @Transactional
    public Rotation rotate(String rawToken, String userAgent, String ip) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid session. Please sign in again."));

        if (current.isRevoked()) {
            // Reuse of a revoked token → likely theft. Nuke the whole family.
            // Done in a separate (REQUIRES_NEW) transaction so it survives the
            // rollback triggered by the exception we are about to throw.
            revoker.revokeFamily(current.getFamily());
            throw new UnauthorizedException("Session is no longer valid. Please sign in again.");
        }
        if (current.isExpired()) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }

        current.setRevokedAt(Instant.now());
        User user = current.getUser();
        String rawNext = persist(user, current.getFamily(), current.getId(), userAgent, ip);
        return new Rotation(user, rawNext);
    }

    /**
     * Revokes the entire family that a raw token belongs to (used at logout).
     * No-op if the token is unknown.
     *
     * @param rawToken the raw token from the client's cookie
     */
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> revoker.revokeFamily(token.getFamily()));
    }

    /** Creates and stores a new refresh-token row, returning its raw value. */
    private String persist(User user, UUID family, UUID parentId, String userAgent, String ip) {
        String raw = generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setFamily(family);
        token.setParentId(parentId);
        token.setExpiresAt(Instant.now().plus(jwtProperties.refreshTtl()));
        token.setUserAgent(userAgent);
        token.setIp(ip);
        repository.save(token);
        return raw;
    }

    /** Generates a 256-bit, URL-safe random token. */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Returns the hex SHA-256 of a raw token (what we actually persist). */
    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Result of a successful rotation.
     *
     * @param user     the token owner
     * @param rawToken the newly issued raw refresh token
     */
    public record Rotation(User user, String rawToken) {
    }
}
