package com.evently.service;

import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.User;
import com.evently.repo.EventRepository;
import com.evently.repo.UserRepository;
import com.evently.web.dto.event.CreateEventRequest;
import com.evently.web.dto.event.CreateTicketTypeRequest;
import com.evently.web.dto.event.EventDetailsResponse;
import com.evently.web.dto.event.EventSummaryResponse;
import com.evently.web.dto.event.UpdateEventRequest;
import com.evently.web.dto.event.UpdateTicketTypeRequest;
import com.evently.web.error.BadRequestException;
import com.evently.web.error.ResourceNotFoundException;
import com.evently.web.mapper.EventMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Organizer-facing event management. Every operation is scoped to the
 * authenticated organizer: lookups go through owner-qualified queries, so one
 * organizer can never read or mutate another's events (a wrong-owner id simply
 * behaves as "not found").
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    /**
     * @param eventRepository event persistence
     * @param userRepository  used to reference the organizer on create
     * @param eventMapper     entity → DTO mapping
     */
    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        EventMapper eventMapper){
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Creates an event (with its ticket tiers) owned by the given organizer.
     *
     * @param organizerId the authenticated organizer's id
     * @param request     the validated create payload
     * @return the created event, including generated ids and timestamps
     */
    @Transactional
    public EventDetailsResponse create(UUID organizerId, CreateEventRequest request){
        validateDates(request.start(), request.end(), request.salesStart(), request.salesEnd());

        User organizer = userRepository.getReferenceById(organizerId);
        Event event = new Event();
        event.setName(request.name());
        event.setStart(request.start());
        event.setEnd(request.end());
        event.setVenue(request.venue());
        event.setSalesStart(request.salesStart());
        event.setSalesEnd(request.salesEnd());
        event.setStatus(request.status());
        event.setOrganizer(organizer);

        for(CreateTicketTypeRequest tt : request.ticketTypes()){
            event.addTicketType(newTicketType(tt.name(), tt.price(), tt.description(), tt.totalAvailable()));
        }

        Event saved = eventRepository.save(event);
        return eventMapper.toDetails(saved);
    }

    /**
     * Lists the organizer's events, newest first.
     *
     * @param organizerId the authenticated organizer's id
     * @param pageable    page/size (and optional sort) from the request
     * @return one page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> list(UUID organizerId, Pageable pageable){
        return eventRepository.findByOrganizerId(organizerId, pageable)
                .map(eventMapper::toSummary);
    }

    /**
     * Fetches one of the organizer's events.
     *
     * @param organizerId the authenticated organizer's id
     * @param eventId     the event to fetch
     * @return the event details
     * @throws ResourceNotFoundException if the event doesn't exist or belongs to someone else
     */
    @Transactional(readOnly = true)
    public EventDetailsResponse get(UUID organizerId, UUID eventId){
        return eventMapper.toDetails(ownedEvent(organizerId, eventId));
    }

    /**
     * Full update of an event, reconciling its ticket tiers against the
     * requested set: tiers with a known id are updated in place, tiers without
     * an id are created, and existing tiers missing from the request are
     * removed (the database refuses removal once tickets are sold — surfaced
     * as a 409).
     *
     * @param organizerId the authenticated organizer's id
     * @param eventId     the event to update (must equal {@code request.id()})
     * @param request     the validated update payload
     * @return the updated event details
     * @throws BadRequestException       if ids are inconsistent or dates invalid
     * @throws ResourceNotFoundException if the event isn't the organizer's
     */
    @Transactional
    public EventDetailsResponse update(UUID organizerId, UUID eventId, UpdateEventRequest request){
        if(!eventId.equals(request.id())){
            throw new BadRequestException("Event id in the path and body must match.");
        }
        validateDates(request.start(), request.end(), request.salesStart(), request.salesEnd());

        Event event = ownedEvent(organizerId, eventId);
        event.setName(request.name());
        event.setStart(request.start());
        event.setEnd(request.end());
        event.setVenue(request.venue());
        event.setSalesStart(request.salesStart());
        event.setSalesEnd(request.salesEnd());
        event.setStatus(request.status());

        reconcileTicketTypes(event, request.ticketTypes());
        return eventMapper.toDetails(event);
    }

    /**
     * Deletes one of the organizer's events (cascades to its ticket tiers; the
     * database refuses if tickets have been sold — surfaced as a 409).
     *
     * @param organizerId the authenticated organizer's id
     * @param eventId     the event to delete
     * @throws ResourceNotFoundException if the event isn't the organizer's
     */
    @Transactional
    public void delete(UUID organizerId, UUID eventId){
        Event event = ownedEvent(organizerId, eventId);
        eventRepository.delete(event);
    }

    /** Loads an event scoped to its owner, or 404s. */
    private Event ownedEvent(UUID organizerId, UUID eventId){
        return eventRepository.findByIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));
    }

    /**
     * Brings the event's tiers in line with the requested set (update / add /
     * remove). Works on the managed collection so JPA's cascade and orphan
     * removal turn the diff into the right SQL.
     */
    private void reconcileTicketTypes(Event event, List<UpdateTicketTypeRequest> requested){
        Map<UUID, TicketType> existingById = new HashMap<>();
        for(TicketType tt : event.getTicketTypes()){
            existingById.put(tt.getId(), tt);
        }

        for(UpdateTicketTypeRequest tt : requested){
            if(tt.id() == null){
                event.addTicketType(newTicketType(tt.name(), tt.price(), tt.description(), tt.totalAvailable()));
            } else {
                TicketType existing = existingById.remove(tt.id());
                if(existing == null){
                    throw new BadRequestException("Unknown ticket type id: " + tt.id());
                }
                existing.setName(tt.name());
                existing.setPrice(tt.price());
                existing.setDescription(tt.description());
                existing.setTotalAvailable(tt.totalAvailable());
            }
        }

        // Whatever wasn't referenced in the request gets removed (orphanRemoval).
        for(TicketType leftover : existingById.values()){
            event.removeTicketType(leftover);
        }
    }

    private TicketType newTicketType(String name, java.math.BigDecimal price,
                                     String description, Integer totalAvailable){
        TicketType ticketType = new TicketType();
        ticketType.setName(name);
        ticketType.setPrice(price);
        ticketType.setDescription(description);
        ticketType.setTotalAvailable(totalAvailable);
        return ticketType;
    }

    /** Rejects date ranges that end before they start. */
    private void validateDates(LocalDateTime start, LocalDateTime end,
                               LocalDateTime salesStart, LocalDateTime salesEnd){
        if(start != null && end != null && end.isBefore(start)){
            throw new BadRequestException("Event end must be after its start.");
        }
        if(salesStart != null && salesEnd != null && salesEnd.isBefore(salesStart)){
            throw new BadRequestException("Sales end must be after sales start.");
        }
    }
}
