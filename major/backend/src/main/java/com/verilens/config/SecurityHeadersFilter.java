package com.verilens.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter — Phase 14 hardening.
 * Adds standard security headers to all HTTP responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // XSS protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer policy — don't leak full URLs to external origins
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Content Security Policy — restrict resource loading
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                + "script-src 'self'; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "font-src 'self' https://fonts.gstatic.com; "
                + "img-src 'self' data:; "
                + "connect-src 'self'; "
                + "frame-ancestors 'none'");

        // Permissions policy — restrict browser features
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Strict Transport Security (effective only over HTTPS in production)
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // Cache control for API responses — no caching of sensitive data
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }
}

