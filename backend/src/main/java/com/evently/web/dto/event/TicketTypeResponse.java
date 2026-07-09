package com.evently.web.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A ticket type as returned to organizers (summary and detail views share the
 * same shape).
 *
 * @param id             the ticket type id
 * @param name           the ticket type name (e.g. "General", "VIP")
 * @param price          the price
 * @param description    optional description
 * @param totalAvailable optional capacity ({@code null} = unlimited)
 */
public record TicketTypeResponse(
        UUID id,
        String name,
        BigDecimal price,
        String description,
        Integer totalAvailable
) {
}
