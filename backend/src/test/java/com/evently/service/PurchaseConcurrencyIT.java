package com.evently.service;

import com.evently.AbstractIntegrationTest;
import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.User;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.domain.enums.RoleEnum;
import com.evently.repo.EventRepository;
import com.evently.repo.TicketRepository;
import com.evently.repo.UserRepository;
import com.evently.web.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the purchase invariants against real Postgres:
 * <ul>
 *   <li>a stampede of concurrent buyers can never oversell a tier,</li>
 *   <li>an Idempotency-Key replay returns the original ticket instead of a
 *       second one,</li>
 *   <li>the sales window is enforced.</li>
 * </ul>
 * The concurrency test is the load test in miniature: 50 threads are released
 * simultaneously by a latch against a 10-seat tier, and exactly 10 must win.
 * Threads call the service directly, so each purchase runs in its own
 * transaction exactly as it would under HTTP.
 */
class PurchaseConcurrencyIT extends AbstractIntegrationTest {

    private static final int SEATS = 10;
    private static final int BUYERS = 50;

    @Autowired
    private TicketPurchaseService ticketPurchaseService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void concurrentBuyersNeverOversell() throws InterruptedException {
        User attendee = createUser("race");
        TicketType tier = createPublishedTier("Race Gala", SEATS, null, null);

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(BUYERS);
        AtomicInteger purchased = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for(int i = 0; i < BUYERS; i++){
            pool.submit(() -> {
                try {
                    startGun.await();
                    ticketPurchaseService.purchase(attendee.getId(), tier.getEvent().getId(), tier.getId(), null);
                    purchased.incrementAndGet();
                } catch(ConflictException e){
                    rejected.incrementAndGet();
                } catch(Exception ignored){
                    // Any other failure counts as neither; the assertions below
                    // will expose it through the mismatch.
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertThat(finished.await(60, TimeUnit.SECONDS)).as("all buyers finished in time").isTrue();
        pool.shutdown();

        assertThat(purchased.get()).as("winners").isEqualTo(SEATS);
        assertThat(rejected.get()).as("sold-out rejections").isEqualTo(BUYERS - SEATS);
        assertThat(ticketRepository.countByTicketTypeId(tier.getId()))
                .as("tickets actually persisted")
                .isEqualTo(SEATS);
    }

    @Test
    void idempotencyKeyReplaysOriginalTicket(){
        User attendee = createUser("idem");
        TicketType tier = createPublishedTier("Idem Show", 5, null, null);
        String key = UUID.randomUUID().toString();

        var first = ticketPurchaseService.purchase(attendee.getId(), tier.getEvent().getId(), tier.getId(), key);
        var second = ticketPurchaseService.purchase(attendee.getId(), tier.getEvent().getId(), tier.getId(), key);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.ticket().getId()).isEqualTo(first.ticket().getId());
        assertThat(ticketRepository.countByTicketTypeId(tier.getId())).isEqualTo(1);
    }

    @Test
    void purchaseOutsideSalesWindowIsRejected(){
        User attendee = createUser("window");
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        TicketType notYetOnSale = createPublishedTier("Future Sale", 5, tomorrow, tomorrow.plusDays(7));

        assertThatThrownBy(() -> ticketPurchaseService.purchase(
                attendee.getId(), notYetOnSale.getEvent().getId(), notYetOnSale.getId(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not started");
    }

    /** Persists a throwaway attendee with a unique email. */
    private User createUser(String prefix){
        User user = new User();
        user.setEmail(prefix + "-" + UUID.randomUUID() + "@evently.test");
        user.setName("Buyer");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles(java.util.EnumSet.of(RoleEnum.ATTENDEE));
        return userRepository.save(user);
    }

    /**
     * Persists a PUBLISHED event owned by a fresh organizer with a single tier
     * of the given capacity and optional sales window, returning the tier.
     */
    private TicketType createPublishedTier(String eventName, int capacity,
                                           LocalDateTime salesStart, LocalDateTime salesEnd){
        User organizer = new User();
        organizer.setEmail("org-" + UUID.randomUUID() + "@evently.test");
        organizer.setName("Organizer");
        organizer.setPasswordHash(passwordEncoder.encode("password123"));
        organizer.setRoles(java.util.EnumSet.of(RoleEnum.ORGANIZER));
        userRepository.save(organizer);

        Event event = new Event();
        event.setName(eventName + " " + UUID.randomUUID().toString().substring(0, 8));
        event.setVenue("Test Hall");
        event.setStatus(EventStatusEnum.PUBLISHED);
        event.setSalesStart(salesStart);
        event.setSalesEnd(salesEnd);
        event.setOrganizer(organizer);

        TicketType tier = new TicketType();
        tier.setName("General");
        tier.setPrice(new BigDecimal("30.00"));
        tier.setDescription("Standard entry");
        tier.setTotalAvailable(capacity);
        event.addTicketType(tier);

        eventRepository.save(event);
        return event.getTicketTypes().get(0);
    }
}
