package com.evently.service;

import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.repo.EventRepository;
import com.evently.repo.TicketRepository;
import com.evently.web.dto.publishedevent.PublishedEventDetailsResponse;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import com.evently.web.dto.publishedevent.PublishedEventTicketTypeResponse;
import com.evently.web.error.ResourceNotFoundException;
import com.evently.web.mapper.PublishedEventMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
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
    private final TicketRepository ticketRepository;
    private final PublishedEventMapper publishedEventMapper;

    /**
     * @param eventRepository      event persistence (status-scoped finders)
     * @param ticketRepository     sold-count lookups for remaining capacity
     * @param publishedEventMapper entity → public DTO mapping
     */
    public PublishedEventService(EventRepository eventRepository,
                                 TicketRepository ticketRepository,
                                 PublishedEventMapper publishedEventMapper){
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
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
        Event event = eventRepository.findByIdAndStatus(eventId, EventStatusEnum.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        List<PublishedEventTicketTypeResponse> tiers = event.getTicketTypes().stream()
                .sorted(Comparator.comparing(TicketType::getPrice))
                .map(this::toPublicTier)
                .toList();

        return new PublishedEventDetailsResponse(
                event.getId(),
                event.getName(),
                event.getStart(),
                event.getEnd(),
                event.getVenue(),
                event.getOrganizer().getName(),
                tiers);
    }

    /**
     * Maps a tier to its public shape, computing how many admissions are still
     * available. Unlimited tiers ({@code totalAvailable == null}) report a null
     * remaining; otherwise remaining is capacity minus tickets already sold,
     * floored at zero so a sold-out tier never shows a negative count.
     */
    private PublishedEventTicketTypeResponse toPublicTier(TicketType tier){
        Integer remaining = null;
        if(tier.getTotalAvailable() != null){
            long sold = ticketRepository.countByTicketTypeId(tier.getId());
            remaining = (int) Math.max(0, tier.getTotalAvailable() - sold);
        }
        return new PublishedEventTicketTypeResponse(
                tier.getId(),
                tier.getName(),
                tier.getPrice(),
                tier.getDescription(),
                tier.getTotalAvailable(),
                remaining);
    }
}
