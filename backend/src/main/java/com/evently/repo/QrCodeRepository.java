package com.evently.repo;

import com.evently.domain.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link QrCode}. Look up by ticket for QR rendering, or by
 * value when validating a scanned code at entry.
 */
public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    /** The QR code belonging to a given ticket. */
    Optional<QrCode> findByTicketId(UUID ticketId);

    /** The QR code matching a scanned/entered payload value. */
    Optional<QrCode> findByValue(String value);
}
