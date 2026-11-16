package com.verilens.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter — Phase 14 hardening.
 * Limits each IP to a configurable number of requests per minute.
 * Uses a sliding window approximation with per-minute buckets.
 *
 * In production, this should be replaced with a distributed rate limiter
 * (Redis, API gateway, or cloud load balancer).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /**
     * Max requests per minute per IP for general endpoints.
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    /**
     * Max login/register attempts per minute per IP (more restrictive).
     */
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;

    /**
     * Stores request counts per IP per minute bucket.
     * Key: "IP:minuteBucket" → count
     */
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    /**
     * Tracks the last cleanup time to periodically purge stale entries.
     */
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        long currentMinute = System.currentTimeMillis() / 60_000;
        String key = clientIp + ":" + currentMinute;

        // Periodic cleanup of old entries (every 2 minutes)
        if (System.currentTimeMillis() - lastCleanup > 120_000) {
            cleanup(currentMinute);
        }

        AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        // Determine rate limit based on endpoint
        String path = request.getRequestURI();
        int limit = isAuthEndpoint(path) ? MAX_AUTH_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));

        if (currentCount > limit) {
            log.warn("Rate limit exceeded for IP {} on {} ({}/{})", clientIp, path, currentCount, limit);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Rate limit exceeded. Please try again later.\","
                    + "\"path\":\"" + path + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanup(long currentMinute) {
        lastCleanup = System.currentTimeMillis();
        // Remove entries older than 2 minutes
        requestCounts.entrySet().removeIf(entry -> {
            try {
                long entryMinute = Long.parseLong(entry.getKey().split(":")[1]);
                return currentMinute - entryMinute > 2;
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }
}

