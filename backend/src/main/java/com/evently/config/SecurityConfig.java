package com.evently.config;

import com.evently.security.JwtAuthenticationFilter;
import com.evently.security.JwtService;
import com.evently.security.RateLimitFilter;
import com.evently.web.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central security configuration.
 * <p>
 * The API is stateless: there is no session, CSRF is disabled, and every request
 * authenticates from a Bearer access token via {@link JwtAuthenticationFilter}.
 * A {@link RateLimitFilter} runs first to throttle the auth endpoints.
 * Fine-grained role checks are applied per-endpoint with method security
 * ({@code @PreAuthorize}); this class only declares which paths are public.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /**
     * @param jwtService          verifies access tokens for the auth filter
     * @param rateLimitProperties configures the auth-endpoint rate limiter
     * @param objectMapper        renders JSON error bodies for auth failures
     */
    public SecurityConfig(JwtService jwtService,
                          RateLimitProperties rateLimitProperties,
                          ObjectMapper objectMapper){
        this.jwtService = jwtService;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the stateless filter chain: public paths for auth, published-event
     * browsing, health, metrics and API docs; everything else authenticated.
     *
     * @param http the security builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimitProperties, objectMapper);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /me needs a valid access token; the rest of /auth is public.
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/published-events/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 401 handler for unauthenticated access to a protected resource. */
    private AuthenticationEntryPoint unauthorizedEntryPoint(){
        return (request, response, authException) ->
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "You need to sign in to continue.");
    }

    /** 403 handler for authenticated-but-forbidden access. */
    private AccessDeniedHandler accessDeniedHandler(){
        return (request, response, accessDeniedException) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "You don't have permission to perform that action.");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(message));
    }
}
