package com.evently.web.dto.publishedevent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A ticket tier as shown to attendees on the public event page. Capacity is
 * surfaced as {@code remaining} (how many are still purchasable) rather than
 * the raw internal figures, so attendees see "43 left" without learning the
 * organizer's exact allocation history; {@code totalAvailable} is included for
 * context. Both are {@code null} for tiers with unlimited capacity.
 *
 * @param id             the ticket type id (used when purchasing)
 * @param name           the tier name shown on the purchase page
 * @param price          the price an attendee will pay
 * @param description    short marketing description of the tier
 * @param totalAvailable the tier's total capacity, or {@code null} if unlimited
 * @param remaining      how many are still available, or {@code null} if unlimited
 */
public record PublishedEventTicketTypeResponse(
        UUID id,
        String name,
        BigDecimal price,
        String description,
        Integer totalAvailable,
        Integer remaining
){
}
