package com.evently.web.dto.publishedevent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A published event as it appears in the public browse/search listing.
 * Mirrors the frontend's {@code PublishedEventSummary}: just enough to render
 * an event card — the full tier list is only fetched on the detail page.
 *
 * @param id    the event id
 * @param name  the event title
 * @param start optional event start
 * @param end   optional event end
 * @param venue where the event takes place
 */
public record PublishedEventSummaryResponse(
        UUID id,
        String name,
        LocalDateTime start,
        LocalDateTime end,
        String venue
){
}
