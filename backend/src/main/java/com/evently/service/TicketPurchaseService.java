package com.evently.service;

import com.evently.domain.Event;
import com.evently.domain.IdempotencyRecord;
import com.evently.domain.Ticket;
import com.evently.domain.TicketType;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.domain.enums.TicketStatusEnum;
import com.evently.repo.IdempotencyRepository;
import com.evently.repo.TicketRepository;
import com.evently.repo.TicketTypeRepository;
import com.evently.repo.UserRepository;
import com.evently.web.error.ConflictException;
import com.evently.web.error.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * The ticket purchase path — the system's contention hotspot, built around one
 * hard invariant: <strong>never sell more than {@code totalAvailable}</strong>,
 * no matter how many buyers race for the last seat.
 *
 * <h4>How oversell is prevented</h4>
 * The check-then-insert ("count sold, then create a ticket") is only safe if no
 * other transaction can run the same check concurrently. The tier row is
 * therefore locked with {@code SELECT ... FOR UPDATE}
 * ({@link TicketTypeRepository#findByIdForUpdate}): competing purchase
 * transactions queue on the row lock and observe each other's committed
 * inserts, so the capacity check is effectively serialized per tier while
 * different tiers still sell fully in parallel.
 *
 * <h4>How retries are made safe</h4>
 * A client that times out and retries must not buy two tickets. Callers may
 * send an {@code Idempotency-Key} header; the (user, key) pair is stored with
 * the resulting ticket id, and a replay short-circuits to the original ticket
 * before any locking happens. If two requests with the same key race, the
 * database's unique constraint lets exactly one create a ticket — the loser
 * gets a 409 and its retry lands on the replay path.
 */
@Service
public class TicketPurchaseService {

    private static final String PURCHASED_COUNTER = "evently.tickets.purchased";
    private static final String PURCHASE_TIMER = "evently.purchase.latency";

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final MeterRegistry meterRegistry;

    /**
     * @param ticketTypeRepository  tier persistence (owns the FOR UPDATE lookup)
     * @param ticketRepository      ticket persistence and sold-count query
     * @param idempotencyRepository replay bookkeeping for Idempotency-Key
     * @param userRepository        used to reference the purchaser
     * @param qrCodeService         creates the ticket's QR credential in-transaction
     * @param meterRegistry         purchase counter and latency timer
     */
    public TicketPurchaseService(TicketTypeRepository ticketTypeRepository,
                                 TicketRepository ticketRepository,
                                 IdempotencyRepository idempotencyRepository,
                                 UserRepository userRepository,
                                 QrCodeService qrCodeService,
                                 MeterRegistry meterRegistry){
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketRepository = ticketRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Purchases one ticket of the given tier for the given attendee.
     *
     * @param attendeeId     the authenticated buyer's id
     * @param eventId        the event the tier must belong to
     * @param ticketTypeId   the tier being bought
     * @param idempotencyKey optional client retry key; {@code null} disables replay
     * @return the purchased (or replayed) ticket plus whether it was a replay
     * @throws ResourceNotFoundException if the tier/event pairing doesn't exist
     *                                   or the event isn't published
     * @throws ConflictException         if sales haven't opened, have closed,
     *                                   or the tier is sold out
     */
    @Transactional
    public PurchaseOutcome purchase(UUID attendeeId, UUID eventId, UUID ticketTypeId, String idempotencyKey){
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Replays return the original ticket without ever touching the lock,
            // so a retry storm cannot pile onto the hot row.
            Optional<Ticket> replayed = findReplay(attendeeId, idempotencyKey);
            if(replayed.isPresent()){
                return new PurchaseOutcome(replayed.get(), true);
            }

            TicketType tier = lockAndValidateTier(eventId, ticketTypeId);
            enforceCapacity(tier);

            Ticket ticket = new Ticket();
            ticket.setStatus(TicketStatusEnum.PURCHASED);
            ticket.setTicketType(tier);
            ticket.setPurchaser(userRepository.getReferenceById(attendeeId));
            ticketRepository.save(ticket);
            qrCodeService.createFor(ticket);

            recordIdempotency(attendeeId, idempotencyKey, ticket);
            meterRegistry.counter(PURCHASED_COUNTER).increment();
            return new PurchaseOutcome(ticket, false);
        } finally {
            sample.stop(meterRegistry.timer(PURCHASE_TIMER));
        }
    }

    /** Looks up a previous result for this (user, key), if the caller sent a key. */
    private Optional<Ticket> findReplay(UUID attendeeId, String idempotencyKey){
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            return Optional.empty();
        }
        return idempotencyRepository.findByUserIdAndIdempotencyKey(attendeeId, idempotencyKey.trim())
                .map(IdempotencyRecord::getTicketId)
                .flatMap(ticketRepository::findById);
    }

    /**
     * Acquires the row lock on the tier and checks everything that must hold
     * before selling: the tier belongs to the event, the event is published,
     * and the current time is inside the sales window (when one is set).
     * <p>
     * Lock first, validate second — validation reads the tier's event, and
     * doing that under the lock guarantees we validate against the state no
     * concurrent writer can be mutating.
     */
    private TicketType lockAndValidateTier(UUID eventId, UUID ticketTypeId){
        TicketType tier = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found."));

        Event event = tier.getEvent();
        if(!event.getId().equals(eventId) || event.getStatus() != EventStatusEnum.PUBLISHED){
            // A tier on a draft/cancelled event is as good as nonexistent to buyers;
            // same for a tier reached through the wrong event id.
            throw new ResourceNotFoundException("Ticket type not found.");
        }

        LocalDateTime now = LocalDateTime.now();
        if(event.getSalesStart() != null && now.isBefore(event.getSalesStart())){
            throw new ConflictException("Ticket sales have not started yet.");
        }
        if(event.getSalesEnd() != null && now.isAfter(event.getSalesEnd())){
            throw new ConflictException("Ticket sales have ended.");
        }
        return tier;
    }

    /** Rejects the purchase when the tier has a capacity and it is exhausted. */
    private void enforceCapacity(TicketType tier){
        if(tier.getTotalAvailable() == null){
            return;
        }
        long sold = ticketRepository.countByTicketTypeId(tier.getId());
        if(sold >= tier.getTotalAvailable()){
            throw new ConflictException("This ticket type is sold out.");
        }
    }

    /** Persists the (user, key) → ticket mapping when a key was supplied. */
    private void recordIdempotency(UUID attendeeId, String idempotencyKey, Ticket ticket){
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.setUserId(attendeeId);
        record.setIdempotencyKey(idempotencyKey.trim());
        record.setTicketId(ticket.getId());
        idempotencyRepository.save(record);
    }

    /**
     * A purchase result: the ticket plus whether it came from the idempotency
     * replay path (controllers use this to pick 200 vs 201).
     *
     * @param ticket   the purchased or replayed ticket
     * @param replayed {@code true} when served from an earlier identical request
     */
    public record PurchaseOutcome(Ticket ticket, boolean replayed){
    }
}
