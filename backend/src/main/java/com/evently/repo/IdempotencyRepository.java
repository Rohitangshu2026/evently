package com.evently.repo;

import com.evently.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link IdempotencyRecord}. Looks up a prior result for a
 * (user, key) pair so retried purchases return the original ticket.
 */
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    /** Finds the record for a given user and idempotency key, if any. */
    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
