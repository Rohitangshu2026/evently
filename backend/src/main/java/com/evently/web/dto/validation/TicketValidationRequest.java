package com.evently.web.dto.validation;

import com.evently.domain.enums.TicketValidationMethodEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v1/ticket-validations}. Mirrors the
 * frontend's {@code TicketValidationRequest}. The meaning of {@code id}
 * depends on the method: for QR scans it is the opaque credential read from
 * the QR image; for manual entry it is the ticket's UUID typed by staff.
 *
 * @param id     the scanned QR value, or the ticket id for manual entry
 * @param method how the ticket was presented at the gate
 */
public record TicketValidationRequest(
        @NotBlank String id,
        @NotNull TicketValidationMethodEnum method
){
}
