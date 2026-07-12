package com.evently.service;

import com.evently.AbstractIntegrationTest;
import com.evently.domain.Event;
import com.evently.domain.TicketType;
import com.evently.domain.User;
import com.evently.domain.enums.EventStatusEnum;
import com.evently.domain.enums.RoleEnum;
import com.evently.domain.enums.TicketStatusEnum;
import com.evently.repo.EventRepository;
import com.evently.repo.QrCodeRepository;
import com.evently.repo.UserRepository;
import com.evently.web.dto.ticket.TicketDetailsResponse;
import com.evently.web.dto.ticket.TicketSummaryResponse;
import com.evently.web.error.ResourceNotFoundException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises attendee ticket retrieval against real Postgres: owner scoping,
 * the flattened detail shape, and the QR image path — including decoding the
 * rendered PNG to prove it round-trips to the stored credential.
 */
class TicketServiceIT extends AbstractIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketPurchaseService purchaseService;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String marker;
    private User buyer;
    private User stranger;
    private Event event;
    private UUID ticketId;

    @BeforeEach
    void seed(){
        marker = UUID.randomUUID().toString().substring(0, 8);
        User organizer = createUser("org", RoleEnum.ORGANIZER);
        buyer = createUser("buyer", RoleEnum.ATTENDEE);
        stranger = createUser("stranger", RoleEnum.ATTENDEE);
        event = createPublishedEvent(organizer);
        ticketId = purchaseService
                .purchase(buyer.getId(), event.getId(), event.getTicketTypes().get(0).getId(), null)
                .ticket().getId();
    }

    @Test
    void listShowsOnlyTheCallersTickets(){
        List<UUID> mine = ticketService.list(buyer.getId(), PageRequest.of(0, 10))
                .map(TicketSummaryResponse::id)
                .getContent();
        assertThat(mine).contains(ticketId);

        List<UUID> theirs = ticketService.list(stranger.getId(), PageRequest.of(0, 10))
                .map(TicketSummaryResponse::id)
                .getContent();
        assertThat(theirs).doesNotContain(ticketId);
    }

    @Test
    void detailsFlattenTierAndEventFields(){
        TicketDetailsResponse details = ticketService.get(buyer.getId(), ticketId);

        assertThat(details.status()).isEqualTo(TicketStatusEnum.PURCHASED);
        assertThat(details.price()).isEqualByComparingTo(new BigDecimal("42.00"));
        assertThat(details.eventName()).isEqualTo(event.getName());
        assertThat(details.eventVenue()).isEqualTo(event.getVenue());
    }

    @Test
    void foreignTicketsAreNotFound(){
        assertThatThrownBy(() -> ticketService.get(stranger.getId(), ticketId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ticketService.qrCodePng(stranger.getId(), ticketId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void qrPngDecodesBackToTheStoredCredential() throws Exception{
        byte[] png = ticketService.qrCodePng(buyer.getId(), ticketId);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();

        String decoded = new QRCodeReader()
                .decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))))
                .getText();
        String stored = qrCodeRepository.findByTicketId(ticketId).orElseThrow().getValue();
        assertThat(decoded).isEqualTo(stored);
    }

    /** Persists a throwaway user with the given role. */
    private User createUser(String prefix, RoleEnum role){
        User user = new User();
        user.setEmail(prefix + "-" + marker + "@evently.test");
        user.setName("Ticket IT " + prefix);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles(EnumSet.of(role));
        return userRepository.save(user);
    }

    /** Persists a PUBLISHED event with one 42.00 tier of ample capacity. */
    private Event createPublishedEvent(User organizer){
        Event seeded = new Event();
        seeded.setName("Ticket IT Gig " + marker);
        seeded.setVenue("IT Hall " + marker);
        seeded.setStatus(EventStatusEnum.PUBLISHED);
        seeded.setOrganizer(organizer);

        TicketType tier = new TicketType();
        tier.setName("General");
        tier.setPrice(new BigDecimal("42.00"));
        tier.setDescription("Standard entry");
        tier.setTotalAvailable(100);
        seeded.addTicketType(tier);

        return eventRepository.save(seeded);
    }
}
