package com.sairaj.jobinfra.server.security;

import com.sairaj.jobinfra.server.domain.ApiKeyEntity;
import com.sairaj.jobinfra.server.service.MetricsRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private final MetricsRegistry metricsRegistry;

    public HttpLoggingFilter(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsRegistry.recordRequest(duration);

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            
            String host = request.getHeader("Host");
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");
            String method = request.getMethod();
            String endpoint = request.getRequestURI();
            int status = response.getStatus();

            MDC.put("clientIp", ip);
            if (host != null) MDC.put("hostname", host);
            if (userAgent != null) MDC.put("userAgent", userAgent);
            if (referer != null) MDC.put("referer", referer);
            MDC.put("httpMethod", method);
            MDC.put("endpoint", endpoint);
            MDC.put("responseStatus", String.valueOf(status));
            MDC.put("latencyMs", String.valueOf(duration));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                Object credentials = auth.getCredentials();
                if (credentials instanceof ApiKeyEntity apiKey) {
                    MDC.put("projectId", String.valueOf(apiKey.getProject().getId()));
                    MDC.put("apiKeyId", maskApiKey(apiKey.getKeyHash()));
                } else {
                    MDC.put("authenticatedUserId", auth.getName());
                }
            }

            log.info("HTTP Request Completed");

            // Clean up MDC for these request-specific keys
            MDC.remove("clientIp");
            MDC.remove("hostname");
            MDC.remove("userAgent");
            MDC.remove("referer");
            MDC.remove("httpMethod");
            MDC.remove("endpoint");
            MDC.remove("responseStatus");
            MDC.remove("latencyMs");
            MDC.remove("projectId");
            MDC.remove("apiKeyId");
            MDC.remove("authenticatedUserId");
        }
    }

    private String maskApiKey(String fullHash) {
        if (fullHash == null || fullHash.length() < 8) return "******";
        return fullHash.substring(0, 4) + "****" + fullHash.substring(fullHash.length() - 4);
    }
}
