package com.evently.web.dto.publishedevent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The public detail view of a published event — what an attendee sees before
 * deciding to buy. Mirrors the frontend's {@code PublishedEventDetails}.
 * Organizer-only fields (status, sales window, audit timestamps) are
 * intentionally absent.
 *
 * @param id          the event id
 * @param name        the event title
 * @param start       optional event start
 * @param end         optional event end
 * @param venue       where the event takes place
 * @param ticketTypes the purchasable tiers with attendee-facing pricing
 */
public record PublishedEventDetailsResponse(
        UUID id,
        String name,
        LocalDateTime start,
        LocalDateTime end,
        String venue,
        List<PublishedEventTicketTypeResponse> ticketTypes
){
}
