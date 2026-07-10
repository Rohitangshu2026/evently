package com.evently.web.dto.event;

import com.evently.domain.enums.EventStatusEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code PUT /api/v1/events/{id}}. Field names mirror the
 * frontend's {@code UpdateEventRequest} type exactly. The {@code id} must match
 * the path variable.
 *
 * @param id          the event id (must equal the path id)
 * @param name        the event title
 * @param start       optional event start
 * @param end         optional event end
 * @param venue       where the event takes place
 * @param salesStart  optional moment ticket sales open
 * @param salesEnd    optional moment ticket sales close
 * @param status      lifecycle status
 * @param ticketTypes the full desired set of tiers (missing existing tiers are removed)
 */
public record UpdateEventRequest(
        @NotNull UUID id,
        @NotBlank String name,
        LocalDateTime start,
        LocalDateTime end,
        @NotBlank String venue,
        LocalDateTime salesStart,
        LocalDateTime salesEnd,
        @NotNull EventStatusEnum status,
        @NotEmpty List<@Valid UpdateTicketTypeRequest> ticketTypes
){
}
