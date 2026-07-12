package com.evently.web.dto.ticket;

import com.evently.domain.enums.TicketStatusEnum;

import java.util.UUID;

/**
 * Response to a ticket purchase. Kept minimal on purpose: the client gets the
 * ticket's identity and can fetch the full view (including the QR code) from
 * the ticket endpoints. Idempotent replays return exactly the same body the
 * original request produced.
 *
 * @param id     the purchased ticket's id
 * @param status the ticket status (PURCHASED on a successful buy)
 */
public record PurchaseTicketResponse(
        UUID id,
        TicketStatusEnum status
){
}
