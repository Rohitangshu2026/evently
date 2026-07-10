package com.evently.security;

import com.evently.config.RateLimitProperties;
import com.evently.web.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory, per-IP rate limiter for the sensitive auth endpoints
 * ({@code /auth/login} and {@code /auth/signup}) to blunt brute-force and
 * account-creation abuse. Backed by Bucket4j token buckets.
 * <p>
 * In-memory buckets are adequate for a single instance; a distributed
 * deployment would move these to a shared store (e.g. Redis).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS =
            Set.of("/api/v1/auth/login", "/api/v1/auth/signup");

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int attemptsPerMinute;
    private final ObjectMapper objectMapper;

    /**
     * @param properties   supplies the per-minute attempt allowance
     * @param objectMapper used to render the 429 error body
     */
    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper){
        this.attemptsPerMinute = properties.authAttemptsPerMinute();
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if(!isRateLimited(request)){
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip -> newBucket());
        if(bucket.tryConsume(1)){
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response);
        }
    }

    private boolean isRateLimited(HttpServletRequest request){
        return "POST".equalsIgnoreCase(request.getMethod())
                && RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    private Bucket newBucket(){
        Bandwidth limit = Bandwidth.builder()
                .capacity(attemptsPerMinute)
                .refillGreedy(attemptsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /** Resolves the client IP, honouring a single {@code X-Forwarded-For} hop. */
    private String clientIp(HttpServletRequest request){
        String forwarded = request.getHeader("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()){
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse("Too many attempts. Please try again shortly."));
    }
}
