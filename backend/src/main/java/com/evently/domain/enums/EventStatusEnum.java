package com.evently.domain.enums;

/**
 * Lifecycle status of an {@link com.evently.domain.Event}. Only {@link #PUBLISHED}
 * events are visible to attendees and available for ticket purchase.
 */
public enum EventStatusEnum {
    /** Being edited by the organizer; not publicly visible. */
    DRAFT,
    /** Live and open for ticket purchase. */
    PUBLISHED,
    /** Called off; no longer available. */
    CANCELLED,
    /** Concluded. */
    COMPLETED
}
