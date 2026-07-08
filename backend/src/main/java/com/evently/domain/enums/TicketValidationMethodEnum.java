package com.evently.domain.enums;

/**
 * How a ticket was presented for validation at entry.
 */
public enum TicketValidationMethodEnum {
    /** Staff scanned the ticket's QR code. */
    QR_SCAN,
    /** Staff entered the ticket identifier by hand (scanner fallback). */
    MANUAL
}
