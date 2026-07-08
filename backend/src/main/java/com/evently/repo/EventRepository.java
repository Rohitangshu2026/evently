package com.evently.repo;

import com.evently.domain.Event;
import com.evently.domain.enums.EventStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Event}. Organizer-scoped finders enforce ownership;
 * status-scoped finders back the public published-events browse and search.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

    /** Organizer's own events, paged. */
    Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

    /** A single event, but only if it belongs to the given organizer. */
    Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

    /** Events in a given status (e.g. PUBLISHED), paged. */
    Page<Event> findByStatus(EventStatusEnum status, Pageable pageable);

    /** A single event, but only if it is in the given status. */
    Optional<Event> findByIdAndStatus(UUID id, EventStatusEnum status);

    /** Case-insensitive search over name and venue, restricted to a status. */
    @Query("""
            select e from Event e
            where e.status = :status
              and (lower(e.name) like lower(concat('%', :q, '%'))
                or lower(e.venue) like lower(concat('%', :q, '%')))
            """)
    Page<Event> searchByStatus(@Param("status") EventStatusEnum status,
                               @Param("q") String q,
                               Pageable pageable);
}
