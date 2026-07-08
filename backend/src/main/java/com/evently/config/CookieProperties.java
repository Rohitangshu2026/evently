package com.evently.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.cookie.*} configuration for the refresh-token cookie.
 *
 * @param refreshName name of the cookie that carries the raw refresh token
 * @param secure      whether the cookie requires HTTPS (false for local http dev)
 * @param sameSite    SameSite attribute (e.g. {@code Strict})
 */
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        String refreshName,
        boolean secure,
        String sameSite
) {
}
