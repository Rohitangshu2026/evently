package com.evently.web.mapper;

import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.web.dto.event.EventDetailsResponse;
import com.evently.web.dto.event.EventSummaryResponse;
import com.evently.web.dto.event.TicketTypeResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Compile-time (MapStruct) mapping from JPA entities to the organizer-facing
 * event DTOs. Field names line up one-to-one, so no explicit {@code @Mapping}
 * overrides are needed; nested ticket types map via {@link #toTicketTypeResponse}.
 */
@Mapper
public interface EventMapper {

    /** Maps a ticket-type entity to its response shape. */
    TicketTypeResponse toTicketTypeResponse(TicketType ticketType);

    /** Maps ticket-type entities to response shapes, preserving order. */
    List<TicketTypeResponse> toTicketTypeResponses(List<TicketType> ticketTypes);

    /** Maps an event to the paged-list summary shape. */
    EventSummaryResponse toSummary(Event event);

    /** Maps an event to the full detail shape (includes audit timestamps). */
    EventDetailsResponse toDetails(Event event);
}
