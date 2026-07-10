package com.evently.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds the {@code app.jwt.*} configuration.
 *
 * @param issuer             the {@code iss} claim written into and required on access tokens
 * @param accessTtl          lifetime of a signed access token (short, e.g. 15m)
 * @param refreshTtl         lifetime of a refresh token (long, e.g. 14d)
 * @param privateKeyLocation Spring resource location of the Ed25519 private key (PEM)
 * @param publicKeyLocation  Spring resource location of the Ed25519 public key (PEM)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTtl,
        Duration refreshTtl,
        String privateKeyLocation,
        String publicKeyLocation
){
}
