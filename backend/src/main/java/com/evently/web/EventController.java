package com.evently.web;

import com.evently.security.AuthPrincipal;
import com.evently.service.EventService;
import com.evently.web.dto.event.CreateEventRequest;
import com.evently.web.dto.event.EventDetailsResponse;
import com.evently.web.dto.event.EventSummaryResponse;
import com.evently.web.dto.event.UpdateEventRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Organizer event management at {@code /api/v1/events}. Every endpoint requires
 * the ORGANIZER role; the service layer additionally scopes each operation to
 * the caller's own events.
 */
@RestController
@RequestMapping("/api/v1/events")
@PreAuthorize("hasRole('ORGANIZER')")
public class EventController {

    private final EventService eventService;

    /** @param eventService organizer event operations */
    public EventController(EventService eventService){
        this.eventService = eventService;
    }

    /**
     * Creates an event with its ticket tiers.
     *
     * @param principal the authenticated organizer
     * @param request   the validated create payload
     * @return 201 with the created event and a Location header
     */
    @PostMapping
    public ResponseEntity<EventDetailsResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                       @Valid @RequestBody CreateEventRequest request){
        EventDetailsResponse created = eventService.create(principal.userId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/events/" + created.id()))
                .body(created);
    }

    /**
     * Lists the organizer's events, newest first by default.
     *
     * @param principal the authenticated organizer
     * @param pageable  page/size from the query string
     * @return one page of event summaries (Spring Data page shape)
     */
    @GetMapping
    public Page<EventSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                           Pageable pageable){
        return eventService.list(principal.userId(), pageable);
    }

    /**
     * Fetches one of the organizer's events.
     *
     * @param principal the authenticated organizer
     * @param id        the event id
     * @return the event details
     */
    @GetMapping("/{id}")
    public EventDetailsResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable UUID id){
        return eventService.get(principal.userId(), id);
    }

    /**
     * Full update of an event, including ticket-tier reconciliation.
     *
     * @param principal the authenticated organizer
     * @param id        the event id (must match the body id)
     * @param request   the validated update payload
     * @return the updated event details
     */
    @PutMapping("/{id}")
    public EventDetailsResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                       @PathVariable UUID id,
                                       @Valid @RequestBody UpdateEventRequest request){
        return eventService.update(principal.userId(), id, request);
    }

    /**
     * Deletes one of the organizer's events.
     *
     * @param principal the authenticated organizer
     * @param id        the event id
     * @return 204 on success
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal,
                       @PathVariable UUID id){
        eventService.delete(principal.userId(), id);
    }
}
