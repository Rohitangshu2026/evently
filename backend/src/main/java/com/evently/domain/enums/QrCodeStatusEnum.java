package com.evently.domain.enums;

/**
 * Status of the {@link com.evently.domain.QrCode} attached to a ticket.
 */
public enum QrCodeStatusEnum {
    /** Usable for validation at entry. */
    ACTIVE,
    /** Consumed or no longer valid (e.g. already validated). */
    EXPIRED
}
