package com.evently.security;

import com.evently.config.JwtProperties;
import com.evently.domain.User;
import com.evently.domain.enums.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and verifies stateless access tokens.
 * <p>
 * Tokens are signed with <strong>EdDSA (Ed25519)</strong> — an asymmetric
 * scheme, so only the private key can mint tokens while any holder of the public
 * key can verify them. The keypair is loaded once from PEM resources at startup.
 */
@Service
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String EMAIL_CLAIM = "email";

    private final JwtProperties properties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * @param properties     JWT configuration (issuer, TTLs, key locations)
     * @param resourceLoader used to read the PEM key resources
     */
    public JwtService(JwtProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    /** Loads the Ed25519 keypair from the configured PEM resources. */
    @PostConstruct
    void loadKeys() {
        this.privateKey = loadPrivateKey(properties.privateKeyLocation());
        this.publicKey = loadPublicKey(properties.publicKeyLocation());
    }

    /**
     * Mints a signed access token for the given user.
     *
     * @param user the authenticated user
     * @return a compact, signed JWT
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTtl());
        List<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.EdDSA)
                .compact();
    }

    /**
     * Verifies a token's signature, issuer, and expiry, and extracts the
     * principal.
     *
     * @param token the compact JWT from the Authorization header
     * @return the authenticated principal
     * @throws JwtException if the token is malformed, expired, or fails verification
     */
    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get(EMAIL_CLAIM, String.class);
        Set<RoleEnum> roles = toRoles(claims.get(ROLES_CLAIM, List.class));
        return new AuthPrincipal(userId, email, roles);
    }

    /** @return the configured access-token lifetime in seconds */
    public long getAccessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }

    @SuppressWarnings("unchecked")
    private Set<RoleEnum> toRoles(List<?> raw) {
        if (raw == null) {
            return Set.of();
        }
        return ((List<String>) raw).stream()
                .map(RoleEnum::valueOf)
                .collect(Collectors.toSet());
    }

    private PrivateKey loadPrivateKey(String location) {
        byte[] der = readDer(location);
        try {
            return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load JWT private key from " + location, e);
        }
    }

    private PublicKey loadPublicKey(String location) {
        byte[] der = readDer(location);
        try {
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load JWT public key from " + location, e);
        }
    }

    /** Reads a PEM resource and returns its base64-decoded DER bytes. */
    private byte[] readDer(String location) {
        Resource resource = resourceLoader.getResource(location);
        try {
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read key resource " + location, e);
        }
    }
}
