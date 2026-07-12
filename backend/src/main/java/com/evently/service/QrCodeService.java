package com.evently.service;

import com.evently.domain.QrCode;
import com.evently.domain.Ticket;
import com.evently.domain.enums.QrCodeStatusEnum;
import com.evently.repo.QrCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

    /**
     * Renders a QR credential as a PNG image. Rendering is done on demand
     * rather than stored: the image is a pure function of the value, so
     * persisting bytes would only duplicate state.
     *
     * @param value  the opaque credential to encode
     * @param sizePx edge length of the square image in pixels
     * @return PNG bytes ready to serve as {@code image/png}
     * @throws IllegalStateException if ZXing fails to encode (never expected
     *                               for our fixed-size base64url payloads)
     */
    public byte[] renderPng(String value, int sizePx){
        try {
            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, sizePx, sizePx);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch(Exception e){
            throw new IllegalStateException("Unable to render QR code image", e);
        }
    }

    /** Produces a URL-safe, 256-bit random credential string. */
    private String generateOpaqueValue(){
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
