package com.evently.service;

import com.evently.domain.QrCode;
import com.evently.domain.Ticket;
import com.evently.domain.enums.QrCodeStatusEnum;
import com.evently.repo.QrCodeRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the QR credential attached to every purchased ticket.
 * <p>
 * The QR payload is an <em>opaque bearer token</em> — 256 bits of secure
 * randomness — not encoded ticket data. The scanner sends the value back to
 * the API, which resolves it server-side; nothing about the ticket, event or
 * holder can be read out of the QR image itself, and a forged value has a
 * 2^-256 chance of colliding with a real one (with a database unique
 * constraint closing even that door).
 */
@Service
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /** @param qrCodeRepository QR credential persistence */
    public QrCodeService(QrCodeRepository qrCodeRepository){
        this.qrCodeRepository = qrCodeRepository;
    }

    /**
     * Generates and stores an ACTIVE QR credential for a freshly purchased
     * ticket. Called inside the purchase transaction so a ticket can never be
     * committed without its credential.
     *
     * @param ticket the just-created ticket
     * @return the persisted QR code row
     */
    public QrCode createFor(Ticket ticket){
        QrCode qrCode = new QrCode();
        qrCode.setTicket(ticket);
        qrCode.setStatus(QrCodeStatusEnum.ACTIVE);
        qrCode.setValue(generateOpaqueValue());
        return qrCodeRepository.save(qrCode);
    }

    /** Produces a URL-safe, 256-bit random credential string. */
    private String generateOpaqueValue(){
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
