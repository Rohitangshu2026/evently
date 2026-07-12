package com.evently.web.dto.validation;

import com.evently.domain.enums.TicketValidationStatusEnum;

import java.util.UUID;

/**
 * Result of a validation attempt, shaped for a scanner UI that needs a fast
 * go/no-go answer. Mirrors the frontend's {@code TicketValidationResponse}.
 *
 * @param ticketId the resolved ticket, or {@code null} when the scanned value
 *                 matched nothing (the status is then INVALID)
 * @param status   VALID = admit; EXPIRED = already used; INVALID = not a ticket
 */
public record TicketValidationResponse(
        UUID ticketId,
        TicketValidationStatusEnum status
){
}
