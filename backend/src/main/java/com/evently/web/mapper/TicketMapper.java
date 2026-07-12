package com.evently.web.mapper;

import com.evently.domain.Ticket;
import com.evently.domain.TicketType;
import com.evently.web.dto.ticket.TicketDetailsResponse;
import com.evently.web.dto.ticket.TicketSummaryResponse;
import com.evently.web.dto.ticket.TicketSummaryTicketTypeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapping from {@link Ticket} to the attendee-facing ticket DTOs.
 * The detail mapping walks ticket → tier → event to produce the flattened
 * shape the client renders; the source paths are spelled out explicitly so a
 * renamed entity field breaks the build here rather than silently mapping null.
 */
@Mapper
public interface TicketMapper {

    /** Maps the tier slice embedded in a list entry. */
    TicketSummaryTicketTypeResponse toSummaryTicketType(TicketType ticketType);

    /** Maps a ticket to its "my tickets" list shape. */
    TicketSummaryResponse toSummary(Ticket ticket);

    /** Maps a ticket to the flattened detail shape (tier + event fields inlined). */
    @Mapping(target = "price", source = "ticketType.price")
    @Mapping(target = "description", source = "ticketType.description")
    @Mapping(target = "eventName", source = "ticketType.event.name")
    @Mapping(target = "eventVenue", source = "ticketType.event.venue")
    @Mapping(target = "eventStart", source = "ticketType.event.start")
    @Mapping(target = "eventEnd", source = "ticketType.event.end")
    TicketDetailsResponse toDetails(Ticket ticket);
}
