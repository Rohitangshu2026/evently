package com.evently.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.rate-limit.*} configuration.
 *
 * @param authAttemptsPerMinute allowed requests per minute per client IP on the
 *                              {@code /auth/login} and {@code /auth/signup} endpoints
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int authAttemptsPerMinute
) {
}
