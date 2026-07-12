package com.evently.web.dto.ticket;

import com.evently.domain.enums.TicketStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The attendee's full view of one ticket — deliberately flattened so the
 * client can render the ticket page from a single response instead of walking
 * ticket → tier → event itself. Mirrors the frontend's {@code TicketDetails}.
 *
 * @param id          the ticket id
 * @param status      the ticket status
 * @param price       price of the tier this ticket was bought at
 * @param description the tier description
 * @param eventName   name of the event the ticket admits to
 * @param eventVenue  venue of that event
 * @param eventStart  when the event starts
 * @param eventEnd    when the event ends
 */
public record TicketDetailsResponse(
        UUID id,
        TicketStatusEnum status,
        BigDecimal price,
        String description,
        String eventName,
        String eventVenue,
        LocalDateTime eventStart,
        LocalDateTime eventEnd
){
}
