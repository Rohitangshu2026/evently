package com.evently.domain.enums;

/**
 * The roles a {@link com.evently.domain.User} can hold. A user may hold more
 * than one. Roles drive method-level authorization via {@code hasRole(...)}.
 */
public enum RoleEnum {
    /** Creates and manages events and their ticket types. */
    ORGANIZER,
    /** Browses published events, purchases tickets, and holds them. */
    ATTENDEE,
    /** Validates attendee tickets at event entry. */
    STAFF
}
