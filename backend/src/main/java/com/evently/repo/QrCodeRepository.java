package com.evently.repo;

import com.evently.domain.QrCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link QrCode}. Plain lookups serve QR rendering; the
 * FOR UPDATE variants serve entry validation, where two staff scanning the
 * same ticket at different gates must not both admit it — the row lock makes
 * the check-then-consume atomic, exactly like the purchase path's tier lock.
 */
public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    /** The QR code belonging to a given ticket. */
    Optional<QrCode> findByTicketId(UUID ticketId);

    /** The QR code matching a scanned/entered payload value. */
    Optional<QrCode> findByValue(String value);

    /** Locks and returns the QR code with the given scanned value. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QrCode q where q.value = :value")
    Optional<QrCode> findByValueForUpdate(@Param("value") String value);

    /** Locks and returns the QR code of the given ticket (manual entry path). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QrCode q where q.ticket.id = :ticketId")
    Optional<QrCode> findByTicketIdForUpdate(@Param("ticketId") UUID ticketId);
}
