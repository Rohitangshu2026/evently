package com.evently.domain.enums;

/**
 * Status of an individual {@link com.evently.domain.Ticket} after purchase.
 */
public enum TicketStatusEnum {
    /** Successfully bought and valid for entry. */
    PURCHASED,
    /** Refunded or voided; not valid for entry. */
    CANCELLED
}
