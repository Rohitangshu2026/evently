package com.evently.service;

import com.evently.AbstractIntegrationTest;
import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.User;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.domain.enums.RoleEnum;
import com.evently.domain.enums.TicketValidationMethodEnum;
import com.evently.domain.enums.TicketValidationStatusEnum;
import com.evently.repo.EventRepository;
import com.evently.repo.QrCodeRepository;
import com.evently.repo.TicketValidationRepository;
import com.evently.repo.UserRepository;
import com.evently.web.dto.validation.TicketValidationRequest;
import com.evently.web.dto.validation.TicketValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises gate validation against real Postgres: the admit-once contract,
 * manual entry, unresolvable scans, the audit trail, and — the part that
 * actually needs a database — two gates scanning the same ticket at the same
 * moment, where the row lock must let exactly one admit.
 */
class TicketValidationServiceIT extends AbstractIntegrationTest {

    private static final int CONCURRENT_SCANNERS = 10;

    @Autowired
    private TicketValidationService validationService;

    @Autowired
    private TicketPurchaseService purchaseService;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private TicketValidationRepository validationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String marker;
    private User staff;
    private UUID ticketId;
    private String qrValue;

    @BeforeEach
    void seed(){
        marker = UUID.randomUUID().toString().substring(0, 8);
        User organizer = createUser("org", RoleEnum.ORGANIZER);
        User buyer = createUser("buyer", RoleEnum.ATTENDEE);
        staff = createUser("staff", RoleEnum.STAFF);

        Event event = createPublishedEvent(organizer);
        ticketId = purchaseService
                .purchase(buyer.getId(), event.getId(), event.getTicketTypes().get(0).getId(), null)
                .ticket().getId();
        qrValue = qrCodeRepository.findByTicketId(ticketId).orElseThrow().getValue();
    }

    @Test
    void scanAdmitsOnceThenReportsExpired(){
        TicketValidationResponse first = validationService.validate(staff.getId(), qrScan(qrValue));
        assertThat(first.status()).isEqualTo(TicketValidationStatusEnum.VALID);
        assertThat(first.ticketId()).isEqualTo(ticketId);

        TicketValidationResponse second = validationService.validate(staff.getId(), qrScan(qrValue));
        assertThat(second.status()).isEqualTo(TicketValidationStatusEnum.EXPIRED);

        // Both attempts land in the audit trail.
        assertThat(validationRepository.findByTicketId(ticketId))
                .extracting("status")
                .containsExactlyInAnyOrder(
                        TicketValidationStatusEnum.VALID,
                        TicketValidationStatusEnum.EXPIRED);
    }

    @Test
    void manualEntryValidatesByTicketId(){
        TicketValidationResponse response = validationService.validate(
                staff.getId(),
                new TicketValidationRequest(ticketId.toString(), TicketValidationMethodEnum.MANUAL));

        assertThat(response.status()).isEqualTo(TicketValidationStatusEnum.VALID);
        assertThat(response.ticketId()).isEqualTo(ticketId);
    }

    @Test
    void unresolvableScansAreInvalidNotErrors(){
        // A forged/garbled QR value.
        TicketValidationResponse forged = validationService.validate(staff.getId(), qrScan("not-a-real-credential"));
        assertThat(forged.status()).isEqualTo(TicketValidationStatusEnum.INVALID);
        assertThat(forged.ticketId()).isNull();

        // A staff typo in manual entry (not even a UUID).
        TicketValidationResponse typo = validationService.validate(
                staff.getId(),
                new TicketValidationRequest("oops-not-a-uuid", TicketValidationMethodEnum.MANUAL));
        assertThat(typo.status()).isEqualTo(TicketValidationStatusEnum.INVALID);
        assertThat(typo.ticketId()).isNull();
    }

    @Test
    void concurrentScansAdmitExactlyOnce() throws Exception{
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_SCANNERS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            // submit() (not invokeAll) — invokeAll blocks until completion, and
            // the tasks gate on the latch we only release afterwards.
            List<Future<TicketValidationStatusEnum>> results = new ArrayList<>();
            for(int i = 0; i < CONCURRENT_SCANNERS; i++){
                results.add(pool.submit(() -> {
                    start.await();
                    return validationService.validate(staff.getId(), qrScan(qrValue)).status();
                }));
            }
            start.countDown();

            long admitted = 0;
            long expired = 0;
            for(Future<TicketValidationStatusEnum> result : results){
                switch(result.get(30, TimeUnit.SECONDS)){
                    case VALID -> admitted++;
                    case EXPIRED -> expired++;
                    default -> throw new AssertionError("Unexpected INVALID for a real credential");
                }
            }

            // The row lock guarantees the door opens exactly once.
            assertThat(admitted).isEqualTo(1);
            assertThat(expired).isEqualTo(CONCURRENT_SCANNERS - 1);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Shorthand for a QR-scan request. */
    private TicketValidationRequest qrScan(String value){
        return new TicketValidationRequest(value, TicketValidationMethodEnum.QR_SCAN);
    }

    /** Persists a throwaway user with the given role. */
    private User createUser(String prefix, RoleEnum role){
        User user = new User();
        user.setEmail(prefix + "-" + marker + "@evently.test");
        user.setName("Validation IT " + prefix);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles(EnumSet.of(role));
        return userRepository.save(user);
    }

    /** Persists a PUBLISHED event with one tier of ample capacity. */
    private Event createPublishedEvent(User organizer){
        Event seeded = new Event();
        seeded.setName("Validation IT Gig " + marker);
        seeded.setVenue("Gate Test Hall " + marker);
        seeded.setStatus(EventStatusEnum.PUBLISHED);
        seeded.setOrganizer(organizer);

        TicketType tier = new TicketType();
        tier.setName("General");
        tier.setPrice(new BigDecimal("30.00"));
        tier.setDescription("Standard entry");
        tier.setTotalAvailable(100);
        seeded.addTicketType(tier);

        return eventRepository.save(seeded);
    }
}
