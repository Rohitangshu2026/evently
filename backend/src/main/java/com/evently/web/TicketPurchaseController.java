package com.evently.web;

import com.evently.security.AuthPrincipal;
import com.evently.service.TicketPurchaseService;
import com.evently.web.dto.ticket.PurchaseTicketResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The attendee purchase endpoint. Lives in its own controller (rather than
 * {@link EventController}) because it carries a different role requirement —
 * organizers manage events, attendees buy tickets — and mixing the two
 * class-level guards would force method-level exceptions.
 * <p>
 * Clients are encouraged to send an {@code Idempotency-Key} header (any unique
 * string per purchase attempt, e.g. a UUID minted when the buy button is
 * clicked). Retries of the same attempt then return the original ticket with
 * 200 instead of buying twice.
 */
@RestController
@PreAuthorize("hasRole('ATTENDEE')")
public class TicketPurchaseController {

    private final TicketPurchaseService ticketPurchaseService;

    /** @param ticketPurchaseService the oversell-safe purchase flow */
    public TicketPurchaseController(TicketPurchaseService ticketPurchaseService){
        this.ticketPurchaseService = ticketPurchaseService;
    }

    /**
     * Buys one ticket of the given tier for the authenticated attendee.
     *
     * @param principal      the authenticated buyer
     * @param eventId        the published event the tier belongs to
     * @param ticketTypeId   the tier to buy
     * @param idempotencyKey optional retry key; replays return the original ticket
     * @return 201 with the new ticket (Location points at it), or 200 when the
     *         request was an idempotent replay of an earlier purchase
     */
    @PostMapping("/api/v1/events/{eventId}/ticket-types/{ticketTypeId}/tickets")
    public ResponseEntity<PurchaseTicketResponse> purchase(@AuthenticationPrincipal AuthPrincipal principal,
                                                           @PathVariable UUID eventId,
                                                           @PathVariable UUID ticketTypeId,
                                                           @RequestHeader(name = "Idempotency-Key", required = false)
                                                           String idempotencyKey){
        TicketPurchaseService.PurchaseOutcome outcome =
                ticketPurchaseService.purchase(principal.userId(), eventId, ticketTypeId, idempotencyKey);

        PurchaseTicketResponse body = new PurchaseTicketResponse(
                outcome.ticket().getId(),
                outcome.ticket().getStatus());

        if(outcome.replayed()){
            return ResponseEntity.ok(body);
        }
        return ResponseEntity
                .created(URI.create("/api/v1/tickets/" + body.id()))
                .body(body);
    }
}
