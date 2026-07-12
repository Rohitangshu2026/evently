package com.evently.service;

import com.evently.domain.QrCode;
import com.evently.domain.Ticket;
import com.evently.repo.QrCodeRepository;
import com.evently.repo.TicketRepository;
import com.evently.web.dto.ticket.TicketDetailsResponse;
import com.evently.web.dto.ticket.TicketSummaryResponse;
import com.evently.web.error.ResourceNotFoundException;
import com.evently.web.mapper.TicketMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Attendee-facing ticket retrieval. Every lookup is scoped to the purchaser:
 * asking for someone else's ticket (or its QR code) behaves exactly like
 * asking for one that doesn't exist. The QR image itself is only ever handed
 * to the ticket's owner — it is the entry credential, so serving it to anyone
 * else would be handing out someone's seat.
 */
@Service
public class TicketService {

    /** Rendered QR edge length; large enough to scan reliably from a phone screen. */
    private static final int QR_IMAGE_SIZE_PX = 300;

    private final TicketRepository ticketRepository;
    private final QrCodeRepository qrCodeRepository;
    private final QrCodeService qrCodeService;
    private final TicketMapper ticketMapper;

    /**
     * @param ticketRepository ticket persistence (owner-scoped finders)
     * @param qrCodeRepository QR credential lookup by ticket
     * @param qrCodeService    renders credentials as PNG images
     * @param ticketMapper     entity → DTO mapping
     */
    public TicketService(TicketRepository ticketRepository,
                         QrCodeRepository qrCodeRepository,
                         QrCodeService qrCodeService,
                         TicketMapper ticketMapper){
        this.ticketRepository = ticketRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.qrCodeService = qrCodeService;
        this.ticketMapper = ticketMapper;
    }

    /**
     * Lists the caller's tickets.
     *
     * @param purchaserId the authenticated attendee's id
     * @param pageable    page/size from the request
     * @return one page of the attendee's tickets
     */
    @Transactional(readOnly = true)
    public Page<TicketSummaryResponse> list(UUID purchaserId, Pageable pageable){
        return ticketRepository.findByPurchaserId(purchaserId, pageable)
                .map(ticketMapper::toSummary);
    }

    /**
     * Fetches one of the caller's tickets in the flattened detail shape.
     *
     * @param purchaserId the authenticated attendee's id
     * @param ticketId    the ticket to fetch
     * @return the ticket with tier and event fields inlined
     * @throws ResourceNotFoundException if the ticket doesn't exist or belongs
     *                                   to another user
     */
    @Transactional(readOnly = true)
    public TicketDetailsResponse get(UUID purchaserId, UUID ticketId){
        return ticketMapper.toDetails(ownedTicket(purchaserId, ticketId));
    }

    /**
     * Renders the caller's ticket QR credential as a PNG.
     *
     * @param purchaserId the authenticated attendee's id
     * @param ticketId    the ticket whose QR code to render
     * @return PNG bytes for an {@code image/png} response
     * @throws ResourceNotFoundException if the ticket isn't the caller's or has
     *                                   no QR credential
     */
    @Transactional(readOnly = true)
    public byte[] qrCodePng(UUID purchaserId, UUID ticketId){
        Ticket ticket = ownedTicket(purchaserId, ticketId);
        QrCode qrCode = qrCodeRepository.findByTicketId(ticket.getId())
                .orElseThrow(() -> new ResourceNotFoundException("QR code not found."));
        return qrCodeService.renderPng(qrCode.getValue(), QR_IMAGE_SIZE_PX);
    }

    /** Loads a ticket scoped to its owner, or 404s. */
    private Ticket ownedTicket(UUID purchaserId, UUID ticketId){
        return ticketRepository.findByIdAndPurchaserId(ticketId, purchaserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
    }
}
