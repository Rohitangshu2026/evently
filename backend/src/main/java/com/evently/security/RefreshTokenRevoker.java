package com.evently.security;

import com.evently.domain.RefreshToken;
import com.evently.repo.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Revokes an entire refresh-token family in its own transaction.
 * <p>
 * This is a separate bean on purpose: reuse detection needs the revocation to be
 * <strong>committed</strong> even though the surrounding refresh request is then
 * rejected with an exception (which would otherwise roll the change back).
 * {@link Propagation#REQUIRES_NEW} runs the revocation in an independent
 * transaction that commits regardless of the caller's outcome.
 */
@Service
public class RefreshTokenRevoker {

    private final RefreshTokenRepository repository;

    /** @param repository refresh-token persistence */
    public RefreshTokenRevoker(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Revokes every still-active token in the given family, in a new transaction.
     *
     * @param family the family id to revoke
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID family) {
        List<RefreshToken> active = repository.findByFamilyAndRevokedAtIsNull(family);
        Instant now = Instant.now();
        active.forEach(token -> token.setRevokedAt(now));
        repository.saveAll(active);
    }
}
