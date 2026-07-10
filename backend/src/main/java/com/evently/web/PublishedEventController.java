package com.evently.web;

import com.evently.service.PublishedEventService;
import com.evently.web.dto.publishedevent.PublishedEventDetailsResponse;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public browse and search for published events at
 * {@code /api/v1/published-events}. These endpoints carry no authentication
 * requirement — they are the storefront window — and the security
 * configuration permits them explicitly. Everything else about an event
 * (editing, sales data) stays behind the organizer API.
 */
@RestController
@RequestMapping("/api/v1/published-events")
public class PublishedEventController {

    private final PublishedEventService publishedEventService;

    /** @param publishedEventService read-only published-event queries */
    public PublishedEventController(PublishedEventService publishedEventService){
        this.publishedEventService = publishedEventService;
    }

    /**
     * Lists published events, newest first, optionally filtered by a free-text
     * query. The same endpoint serves both the browse page (no {@code q}) and
     * the search page ({@code ?q=...}), which keeps the frontend to a single
     * code path.
     *
     * @param query    optional text matched against event name and venue
     * @param pageable page/size from the query string
     * @return one page of event summaries (Spring Data page shape)
     */
    @GetMapping
    public Page<PublishedEventSummaryResponse> list(@RequestParam(name = "q", required = false) String query,
                                                    @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                    Pageable pageable){
        return publishedEventService.list(query, pageable);
    }

    /**
     * Fetches the public detail view of one published event.
     *
     * @param id the event id
     * @return the event with its purchasable ticket tiers
     */
    @GetMapping("/{id}")
    public PublishedEventDetailsResponse get(@PathVariable UUID id){
        return publishedEventService.get(id);
    }
}
