package com.evently.web.dto.publishedevent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The public detail view of a published event — what an attendee sees before
 * deciding to buy. Includes the hosting organizer's name for trust, but no
 * other organizer-only fields (status, sales window, audit timestamps).
 *
 * @param id            the event id
 * @param name          the event title
 * @param start         optional event start
 * @param end           optional event end
 * @param venue         where the event takes place
 * @param organizerName display name of the organizer hosting the event
 * @param ticketTypes   the purchasable tiers with attendee-facing pricing
 */
public record PublishedEventDetailsResponse(
        UUID id,
        String name,
        LocalDateTime start,
        LocalDateTime end,
        String venue,
        String organizerName,
        List<PublishedEventTicketTypeResponse> ticketTypes
){
}
