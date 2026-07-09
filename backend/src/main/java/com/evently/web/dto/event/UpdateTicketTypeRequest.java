package com.evently.web.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A ticket type supplied when updating an event. A {@code null} id means
 * "create this tier"; a non-null id must belong to the event being updated.
 * Existing tiers omitted from the update are removed (refused if tickets have
 * already been sold against them).
 *
 * @param id             existing tier id, or {@code null} for a new tier
 * @param name           the tier name
 * @param price          the price; zero is allowed
 * @param description    short description shown to attendees
 * @param totalAvailable optional capacity; {@code null} means unlimited
 */
public record UpdateTicketTypeRequest(
        UUID id,
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotBlank String description,
        @Positive Integer totalAvailable
) {
}
