package com.evently.web.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * A ticket type supplied when creating an event.
 *
 * @param name           the tier name (e.g. "General", "VIP")
 * @param price          the price; zero is allowed (free events)
 * @param description    short description shown to attendees
 * @param totalAvailable optional capacity; {@code null} means unlimited
 */
public record CreateTicketTypeRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotBlank String description,
        @Positive Integer totalAvailable
){
}
