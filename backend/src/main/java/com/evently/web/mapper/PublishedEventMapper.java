package com.evently.web.mapper;

import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.web.dto.publishedevent.PublishedEventDetailsResponse;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import com.evently.web.dto.publishedevent.PublishedEventTicketTypeResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapping from {@link Event} to the attendee-facing published-event
 * DTOs. Kept separate from {@link EventMapper} on purpose: the organizer and
 * public views expose different fields, and a dedicated mapper makes it
 * impossible to accidentally leak an organizer-only field through a shared
 * mapping method.
 */
@Mapper
public interface PublishedEventMapper {

    /** Maps a tier to its public shape (price and description, no capacity). */
    PublishedEventTicketTypeResponse toTicketTypeResponse(TicketType ticketType);

    /** Maps an event to the browse/search card shape. */
    PublishedEventSummaryResponse toSummary(Event event);

    /** Maps an event to the public detail shape including purchasable tiers. */
    PublishedEventDetailsResponse toDetails(Event event);
}
