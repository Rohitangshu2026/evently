package com.evently.web.dto.ticket;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The slice of a ticket tier shown inside a ticket list entry. Mirrors the
 * frontend's {@code TicketSummaryTicketType}.
 *
 * @param id    the tier id
 * @param name  the tier name (e.g. "General")
 * @param price the price paid per ticket of this tier
 */
public record TicketSummaryTicketTypeResponse(
        UUID id,
        String name,
        BigDecimal price
){
}
