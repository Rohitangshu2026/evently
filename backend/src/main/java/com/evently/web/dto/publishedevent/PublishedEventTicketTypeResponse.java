package com.evently.web.dto.publishedevent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A ticket tier as shown to attendees on the public event page. Deliberately
 * narrower than the organizer's view: capacity ({@code totalAvailable}) is an
 * internal number, so it never leaves the API here — attendees only learn a
 * tier is limited when it sells out.
 *
 * @param id          the ticket type id (used when purchasing)
 * @param name        the tier name shown on the purchase page
 * @param price       the price an attendee will pay
 * @param description short marketing description of the tier
 */
public record PublishedEventTicketTypeResponse(
        UUID id,
        String name,
        BigDecimal price,
        String description
){
}
