package com.evently.web.dto.event;

import com.evently.domain.enums.EventStatusEnum;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full event view for the organizer's detail/edit page. Mirrors the frontend's
 * {@code EventDetails} type (summary fields plus audit timestamps).
 *
 * @param id          the event id
 * @param name        the event title
 * @param start       optional event start
 * @param end         optional event end
 * @param venue       the venue
 * @param salesStart  optional sales-open moment
 * @param salesEnd    optional sales-close moment
 * @param status      lifecycle status
 * @param ticketTypes the tiers offered
 * @param createdAt   when the event was created
 * @param updatedAt   when the event was last modified
 */
public record EventDetailsResponse(
        UUID id,
        String name,
        LocalDateTime start,
        LocalDateTime end,
        String venue,
        LocalDateTime salesStart,
        LocalDateTime salesEnd,
        EventStatusEnum status,
        List<TicketTypeResponse> ticketTypes,
        Instant createdAt,
        Instant updatedAt
){
}
