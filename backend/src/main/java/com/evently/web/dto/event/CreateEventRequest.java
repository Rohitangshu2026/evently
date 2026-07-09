package com.evently.web.dto.event;

import com.evently.domain.enums.EventStatusEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/events}. Field names mirror the
 * frontend's {@code CreateEventRequest} type exactly.
 *
 * @param name        the event title
 * @param start       optional event start
 * @param end         optional event end
 * @param venue       where the event takes place
 * @param salesStart  optional moment ticket sales open
 * @param salesEnd    optional moment ticket sales close
 * @param status      initial lifecycle status (usually DRAFT or PUBLISHED)
 * @param ticketTypes at least one ticket tier offered for the event
 */
public record CreateEventRequest(
        @NotBlank String name,
        LocalDateTime start,
        LocalDateTime end,
        @NotBlank String venue,
        LocalDateTime salesStart,
        LocalDateTime salesEnd,
        @NotNull EventStatusEnum status,
        @NotEmpty List<@Valid CreateTicketTypeRequest> ticketTypes
) {
}
