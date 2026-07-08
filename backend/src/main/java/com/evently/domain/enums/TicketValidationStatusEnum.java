package com.evently.domain.enums;

/**
 * Outcome of a ticket-validation attempt at event entry.
 */
public enum TicketValidationStatusEnum {
    /** Ticket was valid and is now admitted. */
    VALID,
    /** Ticket could not be found or is not valid for this event. */
    INVALID,
    /** Ticket was already validated (or otherwise no longer usable). */
    EXPIRED
}
