package com.evently.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Tags every request with a correlation id. An incoming {@code X-Request-Id}
 * header is honoured (so ids can flow through a gateway or from the client);
 * otherwise one is generated. The id is placed in the logging MDC — every log
 * line written while handling the request carries it — and echoed back in the
 * response header so a client-reported failure can be matched to server logs.
 * <p>
 * Runs first in the chain (highest precedence) so even security rejections
 * are correlated, and always clears the MDC afterwards because servlet
 * threads are pooled and reused across requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** Header used both for inbound propagation and the response echo. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** MDC key referenced by the logging pattern in application.yml. */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = normalize(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Uses the caller's id when it looks sane, otherwise generates one. The
     * length cap stops a hostile client stuffing megabytes into every log line.
     */
    private String normalize(String candidate){
        if(candidate == null || candidate.isBlank() || candidate.length() > 64){
            return UUID.randomUUID().toString();
        }
        return candidate.trim();
    }
}
