package com.evently.service;

import com.evently.domain.enums.EventStatusEnum;
import com.evently.repo.EventRepository;
import com.evently.web.dto.publishedevent.PublishedEventDetailsResponse;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import com.evently.web.error.ResourceNotFoundException;
import com.evently.web.mapper.PublishedEventMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read-only, public access to events in the PUBLISHED state. This is the
 * attendee-facing counterpart to {@link EventService}: no authentication, no
 * ownership — but strictly limited to published events, so drafts and
 * cancelled events are invisible here even if their id is known.
 */
@Service
public class PublishedEventService {

    private final EventRepository eventRepository;
    private final PublishedEventMapper publishedEventMapper;

    /**
     * @param eventRepository      event persistence (status-scoped finders)
     * @param publishedEventMapper entity → public DTO mapping
     */
    public PublishedEventService(EventRepository eventRepository,
                                 PublishedEventMapper publishedEventMapper){
        this.eventRepository = eventRepository;
        this.publishedEventMapper = publishedEventMapper;
    }

    /**
     * Lists published events, optionally filtered by a search query. A blank
     * or missing query means "browse everything"; otherwise the query matches
     * case-insensitively against event name and venue, so "hall" finds both
     * "Music Hall Live" and events held at "Riverside Hall".
     *
     * @param query    optional search text; {@code null}/blank disables filtering
     * @param pageable page, size and sort from the request
     * @return one page of published-event summaries
     */
    @Transactional(readOnly = true)
    public Page<PublishedEventSummaryResponse> list(String query, Pageable pageable){
        if(query == null || query.isBlank()){
            return eventRepository.findByStatus(EventStatusEnum.PUBLISHED, pageable)
                    .map(publishedEventMapper::toSummary);
        }
        return eventRepository.searchByStatus(EventStatusEnum.PUBLISHED, query.trim(), pageable)
                .map(publishedEventMapper::toSummary);
    }

    /**
     * Fetches the public detail view of a single published event.
     *
     * @param eventId the event id
     * @return the event with its purchasable tiers
     * @throws ResourceNotFoundException if the event doesn't exist or isn't
     *                                   published — the two cases are deliberately
     *                                   indistinguishable to the caller
     */
    @Transactional(readOnly = true)
    public PublishedEventDetailsResponse get(UUID eventId){
        return eventRepository.findByIdAndStatus(eventId, EventStatusEnum.PUBLISHED)
                .map(publishedEventMapper::toDetails)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));
    }
}
