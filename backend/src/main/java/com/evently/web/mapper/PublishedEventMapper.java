package com.evently.web.mapper;

import com.evently.domain.Event;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapping for the attendee-facing browse listing. The detail view is
 * assembled in {@link com.evently.service.PublishedEventService} instead, since
 * it carries computed per-tier remaining capacity that a plain field mapping
 * can't produce.
 */
@Mapper
public interface PublishedEventMapper {

    /** Maps an event to the browse/search card shape. */
    PublishedEventSummaryResponse toSummary(Event event);
}
