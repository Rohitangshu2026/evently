package com.evently.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests bearing a {@code Authorization: Bearer <token>} header.
 * <p>
 * On a valid token the {@link AuthPrincipal} and its {@code ROLE_*} authorities
 * are placed in the {@link SecurityContextHolder}; on a missing or invalid token
 * the request simply continues unauthenticated, letting authorization rules (or
 * the entry point) decide the outcome.
 * <p>
 * Deliberately not a bean — it is constructed and wired into the chain by
 * {@link com.evently.config.SecurityConfig} to avoid Spring Boot registering it
 * as a servlet filter twice.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /** @param jwtService verifies and parses access tokens */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthPrincipal principal = jwtService.parse(token);
                List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                // Invalid/expired token: leave the context unauthenticated.
                log.debug("Rejected access token: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Extracts the raw JWT from the Authorization header, or {@code null}. */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
