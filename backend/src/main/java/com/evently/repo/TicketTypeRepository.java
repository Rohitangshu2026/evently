package com.evently.repo;

import com.evently.domain.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link TicketType}, including the pessimistic-lock query at
 * the heart of the oversell-safe purchase path.
 */
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    /** A ticket type scoped to its event (validates the event/type pairing). */
    Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);

    /**
     * Locks the ticket-type row FOR UPDATE so concurrent purchases serialize on
     * the inventory check. This is the core of the oversell-prevention path.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tt from TicketType tt where tt.id = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") UUID id);
}
