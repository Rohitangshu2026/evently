package com.evently.web;

import com.evently.security.AuthPrincipal;
import com.evently.service.TicketService;
import com.evently.web.dto.ticket.TicketDetailsResponse;
import com.evently.web.dto.ticket.TicketSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The attendee's own tickets at {@code /api/v1/tickets}. Requires the ATTENDEE
 * role; the service layer additionally scopes every lookup to the caller, so
 * one attendee can never enumerate or view another's tickets or QR codes.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@PreAuthorize("hasRole('ATTENDEE')")
public class TicketController {

    private final TicketService ticketService;

    /** @param ticketService owner-scoped ticket queries */
    public TicketController(TicketService ticketService){
        this.ticketService = ticketService;
    }

    /**
     * Lists the caller's tickets, most recent purchase first.
     *
     * @param principal the authenticated attendee
     * @param pageable  page/size from the query string
     * @return one page of ticket summaries (Spring Data page shape)
     */
    @GetMapping
    public Page<TicketSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable){
        return ticketService.list(principal.userId(), pageable);
    }

    /**
     * Fetches one of the caller's tickets.
     *
     * @param principal the authenticated attendee
     * @param id        the ticket id
     * @return the flattened ticket detail view
     */
    @GetMapping("/{id}")
    public TicketDetailsResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable UUID id){
        return ticketService.get(principal.userId(), id);
    }

    /**
     * Serves the ticket's QR credential as a PNG image. The path is plural
     * ({@code /qr-codes}) to match the frontend's existing API client.
     *
     * @param principal the authenticated attendee
     * @param id        the ticket id
     * @return PNG bytes with an {@code image/png} content type
     */
    @GetMapping(value = "/{id}/qr-codes", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrCode(@AuthenticationPrincipal AuthPrincipal principal,
                         @PathVariable UUID id){
        return ticketService.qrCodePng(principal.userId(), id);
    }
}
