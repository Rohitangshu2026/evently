package com.evently.repo;

import com.evently.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Ticket}. Purchaser-scoped finders enforce that users
 * only see their own tickets; the count backs the inventory check on purchase.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /** A purchaser's own tickets, paged. */
    Page<Ticket> findByPurchaserId(UUID purchaserId, Pageable pageable);

    /** A single ticket, but only if it belongs to the given purchaser. */
    Optional<Ticket> findByIdAndPurchaserId(UUID id, UUID purchaserId);

    /** Number of tickets already sold for a ticket type (used for the oversell check). */
    long countByTicketTypeId(UUID ticketTypeId);
}
