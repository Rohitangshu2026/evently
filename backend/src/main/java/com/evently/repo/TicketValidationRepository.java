package com.evently.repo;

import com.evently.domain.TicketValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Data access for {@link TicketValidation} — the audit trail of entry scans.
 */
public interface TicketValidationRepository extends JpaRepository<TicketValidation, UUID> {
}
