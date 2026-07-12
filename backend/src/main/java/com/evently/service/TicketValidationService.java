package com.evently.service;

import com.evently.domain.QrCode;
import com.evently.domain.Ticket;
import com.evently.domain.TicketValidation;
import com.evently.domain.enums.QrCodeStatusEnum;
import com.evently.domain.enums.TicketStatusEnum;
import com.evently.domain.enums.TicketValidationMethodEnum;
import com.evently.domain.enums.TicketValidationStatusEnum;
import com.evently.repo.QrCodeRepository;
import com.evently.repo.TicketValidationRepository;
import com.evently.repo.UserRepository;
import com.evently.web.dto.validation.TicketValidationRequest;
import com.evently.web.dto.validation.TicketValidationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Entry validation at the gate. A ticket admits exactly once: the first
 * successful scan consumes the QR credential (ACTIVE → EXPIRED) and every
 * later attempt — same gate or another — reports EXPIRED.
 *
 * <h4>Why this locks</h4>
 * Two staff can scan the same ticket at different entrances in the same
 * instant. Both would read the credential as ACTIVE and both would admit —
 * the same lost-update shape as the purchase path. The credential row is
 * therefore read {@code FOR UPDATE}: the second scanner blocks until the
 * first commits, then sees the consumed state and gets EXPIRED.
 *
 * <h4>Outcomes</h4>
 * <ul>
 *   <li><strong>VALID</strong> — credential active, ticket PURCHASED; admitted
 *       and consumed, audit row written.</li>
 *   <li><strong>EXPIRED</strong> — credential already consumed; audit row
 *       written (repeat scans are evidence worth keeping).</li>
 *   <li><strong>INVALID</strong> — nothing resolved (forged/mistyped), or the
 *       ticket was cancelled. Audited only when a real ticket was resolved,
 *       since the audit table's ticket reference is mandatory.</li>
 * </ul>
 */
@Service
public class TicketValidationService {

    private final QrCodeRepository qrCodeRepository;
    private final TicketValidationRepository ticketValidationRepository;
    private final UserRepository userRepository;

    /**
     * @param qrCodeRepository           locked credential lookups
     * @param ticketValidationRepository audit-trail persistence
     * @param userRepository             used to reference the validating staff member
     */
    public TicketValidationService(QrCodeRepository qrCodeRepository,
                                   TicketValidationRepository ticketValidationRepository,
                                   UserRepository userRepository){
        this.qrCodeRepository = qrCodeRepository;
        this.ticketValidationRepository = ticketValidationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validates a presented ticket and records the attempt.
     *
     * @param staffId the authenticated staff member performing the scan
     * @param request the scanned value (or ticket id) and presentation method
     * @return the gate decision; never throws for a bad scan — scanners need
     *         an answer, not an error page
     */
    @Transactional
    public TicketValidationResponse validate(UUID staffId, TicketValidationRequest request){
        Optional<QrCode> resolved = resolveCredential(request);
        if(resolved.isEmpty()){
            return new TicketValidationResponse(null, TicketValidationStatusEnum.INVALID);
        }

        QrCode credential = resolved.get();
        Ticket ticket = credential.getTicket();

        if(ticket.getStatus() != TicketStatusEnum.PURCHASED){
            recordAttempt(staffId, ticket, request.method(), TicketValidationStatusEnum.INVALID);
            return new TicketValidationResponse(ticket.getId(), TicketValidationStatusEnum.INVALID);
        }
        if(credential.getStatus() == QrCodeStatusEnum.EXPIRED){
            recordAttempt(staffId, ticket, request.method(), TicketValidationStatusEnum.EXPIRED);
            return new TicketValidationResponse(ticket.getId(), TicketValidationStatusEnum.EXPIRED);
        }

        credential.setStatus(QrCodeStatusEnum.EXPIRED);
        recordAttempt(staffId, ticket, request.method(), TicketValidationStatusEnum.VALID);
        return new TicketValidationResponse(ticket.getId(), TicketValidationStatusEnum.VALID);
    }

    /**
     * Resolves the presented identifier to a locked credential row. QR scans
     * carry the opaque credential value; manual entry carries the ticket UUID
     * (an unparseable UUID simply resolves to nothing — staff typos are an
     * INVALID answer, not a 400).
     */
    private Optional<QrCode> resolveCredential(TicketValidationRequest request){
        String presented = request.id().trim();
        if(request.method() == TicketValidationMethodEnum.QR_SCAN){
            return qrCodeRepository.findByValueForUpdate(presented);
        }
        return parseUuid(presented).flatMap(qrCodeRepository::findByTicketIdForUpdate);
    }

    /** Writes one audit row for an attempt against a resolved ticket. */
    private void recordAttempt(UUID staffId, Ticket ticket,
                               TicketValidationMethodEnum method,
                               TicketValidationStatusEnum status){
        TicketValidation attempt = new TicketValidation();
        attempt.setTicket(ticket);
        attempt.setMethod(method);
        attempt.setStatus(status);
        attempt.setValidatedBy(userRepository.getReferenceById(staffId));
        ticketValidationRepository.save(attempt);
    }

    /** Parses a UUID leniently, returning empty on malformed input. */
    private Optional<UUID> parseUuid(String raw){
        try {
            return Optional.of(UUID.fromString(raw));
        } catch(IllegalArgumentException e){
            return Optional.empty();
        }
    }
}
