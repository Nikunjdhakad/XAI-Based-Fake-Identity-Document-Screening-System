package com.verilens.controller;

import com.verilens.dto.*;
import com.verilens.service.AuditService;
import com.verilens.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller — registration, login, and profile endpoints.
 * All business logic is in AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    /**
     * POST /api/auth/register — Create a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
                                      HttpServletRequest httpRequest) {
        try {
            UserResponse user = authService.register(request);
            auditService.log(user.getId(), "USER_REGISTERED", "USER", user.getId(),
                    null, httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(409, "Conflict", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * POST /api/auth/login — Authenticate and receive a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.login(request);
            auditService.log(response.getUser().getId(), "LOGIN_SUCCESS",
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            auditService.log(null, "LOGIN_FAILURE", "USER", null,
                    "username=" + request.getUsername(), httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Unauthorized", "Invalid credentials",
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * GET /api/auth/me — Get the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Unauthorized", "Not authenticated", "/api/auth/me"));
        }
        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}
