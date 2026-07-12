package com.evently.web;

import com.evently.security.AuthPrincipal;
import com.evently.service.TicketValidationService;
import com.evently.web.dto.validation.TicketValidationRequest;
import com.evently.web.dto.validation.TicketValidationResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The staff scanning endpoint at {@code /api/v1/ticket-validations}. Requires
 * the STAFF role. Always answers 200 with a gate decision — a scanner held at
 * a busy entrance needs VALID / EXPIRED / INVALID, never an error page.
 */
@RestController
@RequestMapping("/api/v1/ticket-validations")
@PreAuthorize("hasRole('STAFF')")
public class TicketValidationController {

    private final TicketValidationService ticketValidationService;

    /** @param ticketValidationService gate validation and audit logic */
    public TicketValidationController(TicketValidationService ticketValidationService){
        this.ticketValidationService = ticketValidationService;
    }

    /**
     * Validates a presented ticket (QR scan or manual entry).
     *
     * @param principal the authenticated staff member
     * @param request   what was presented and how
     * @return the gate decision with the resolved ticket id when known
     */
    @PostMapping
    public TicketValidationResponse validate(@AuthenticationPrincipal AuthPrincipal principal,
                                             @Valid @RequestBody TicketValidationRequest request){
        return ticketValidationService.validate(principal.userId(), request);
    }
}
