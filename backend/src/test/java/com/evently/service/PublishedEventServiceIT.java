package com.evently.service;

import com.evently.AbstractIntegrationTest;
import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.User;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.domain.enums.RoleEnum;
import com.evently.repo.EventRepository;
import com.evently.repo.UserRepository;
import com.evently.web.dto.publishedevent.PublishedEventDetailsResponse;
import com.evently.web.dto.publishedevent.PublishedEventSummaryResponse;
import com.evently.web.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the public published-events queries against real Postgres.
 * <p>
 * The shared {@code evently_test} database is not wiped between runs, so every
 * event seeded here carries a per-run marker in its name and assertions are
 * scoped to that marker (or to the seeded ids) rather than to absolute counts.
 */
class PublishedEventServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PublishedEventService publishedEventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Per-run marker embedded in seeded names so lookups only see this run's data. */
    private String marker;

    private Event publishedGala;
    private Event publishedJazz;
    private Event draftFestival;

    @BeforeEach
    void seed(){
        marker = UUID.randomUUID().toString().substring(0, 8);
        User organizer = createOrganizer();
        publishedGala = createEvent(organizer, "Gala " + marker, "Riverside Hall", EventStatusEnum.PUBLISHED);
        publishedJazz = createEvent(organizer, "Jazz Night " + marker, "Blue Note " + marker, EventStatusEnum.PUBLISHED);
        draftFestival = createEvent(organizer, "Festival " + marker, "City Park", EventStatusEnum.DRAFT);
    }

    @Test
    void listReturnsOnlyPublishedEvents(){
        List<UUID> listedIds = publishedEventService
                .list(null, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PublishedEventSummaryResponse::id)
                .getContent();

        assertThat(listedIds).contains(publishedGala.getId(), publishedJazz.getId());
        assertThat(listedIds).doesNotContain(draftFestival.getId());
    }

    @Test
    void searchMatchesNameAndVenueCaseInsensitively(){
        // Match by name fragment, deliberately upper-cased.
        List<UUID> byName = publishedEventService
                .list(("gala " + marker).toUpperCase(), PageRequest.of(0, 10))
                .map(PublishedEventSummaryResponse::id)
                .getContent();
        assertThat(byName).containsExactly(publishedGala.getId());

        // Match by venue fragment; the draft's venue must stay invisible.
        List<UUID> byVenue = publishedEventService
                .list("blue note " + marker, PageRequest.of(0, 10))
                .map(PublishedEventSummaryResponse::id)
                .getContent();
        assertThat(byVenue).containsExactly(publishedJazz.getId());
    }

    @Test
    void detailsExposeTiersButNotCapacity(){
        PublishedEventDetailsResponse details = publishedEventService.get(publishedGala.getId());

        assertThat(details.name()).isEqualTo(publishedGala.getName());
        assertThat(details.ticketTypes()).hasSize(1);
        assertThat(details.ticketTypes().get(0).price()).isEqualByComparingTo(new BigDecimal("25.00"));
        // The public tier shape carries no totalAvailable — enforced at compile
        // time by the DTO; this test documents the intent.
    }

    @Test
    void draftAndUnknownEventsAreIndistinguishablyNotFound(){
        assertThatThrownBy(() -> publishedEventService.get(draftFestival.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> publishedEventService.get(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Persists a throwaway organizer to own the seeded events. */
    private User createOrganizer(){
        User user = new User();
        user.setEmail("organizer-" + marker + "@evently.test");
        user.setName("Seed Organizer");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles(java.util.EnumSet.of(RoleEnum.ORGANIZER));
        return userRepository.save(user);
    }

    /** Persists an event with a single 25.00 tier in the given status. */
    private Event createEvent(User organizer, String name, String venue, EventStatusEnum status){
        Event event = new Event();
        event.setName(name);
        event.setVenue(venue);
        event.setStatus(status);
        event.setOrganizer(organizer);

        TicketType tier = new TicketType();
        tier.setName("General");
        tier.setPrice(new BigDecimal("25.00"));
        tier.setDescription("Standard entry");
        tier.setTotalAvailable(100);
        event.addTicketType(tier);

        return eventRepository.save(event);
    }
}
