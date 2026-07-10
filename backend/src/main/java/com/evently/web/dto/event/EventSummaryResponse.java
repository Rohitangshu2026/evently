package com.evently.web.dto.event;

import com.evently.domain.enums.EventStatusEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * An event as it appears in the organizer's paged list. Mirrors the frontend's
 * {@code EventSummary} type.
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
 */
public record EventSummaryResponse(
        UUID id,
        String name,
        LocalDateTime start,
        LocalDateTime end,
        String venue,
        LocalDateTime salesStart,
        LocalDateTime salesEnd,
        EventStatusEnum status,
        List<TicketTypeResponse> ticketTypes
){
}
