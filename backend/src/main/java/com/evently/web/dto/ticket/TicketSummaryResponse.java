package com.evently.web.dto.ticket;

import com.evently.domain.enums.TicketStatusEnum;

import java.util.UUID;

/**
 * A ticket as it appears in the attendee's "my tickets" list. Mirrors the
 * frontend's {@code TicketSummary}.
 *
 * @param id         the ticket id
 * @param status     the ticket status
 * @param ticketType the tier the ticket belongs to
 */
public record TicketSummaryResponse(
        UUID id,
        TicketStatusEnum status,
        TicketSummaryTicketTypeResponse ticketType
){
}
