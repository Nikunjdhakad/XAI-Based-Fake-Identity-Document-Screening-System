package com.verilens.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.dto.LoginRequest;
import com.verilens.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests registration, login, and token-protected endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int testCounter = 0;

    private RegisterRequest createUniqueRegisterRequest() {
        testCounter++;
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser" + testCounter + System.nanoTime());
        request.setEmail("test" + testCounter + System.nanoTime() + "@example.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Test User " + testCounter);
        return request;
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    void register_success_returns201() throws Exception {
        RegisterRequest request = createUniqueRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(request.getUsername()))
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.fullName").value(request.getFullName()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequest first = createUniqueRegisterRequest();

        // Register first user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Try to register with same username
        RegisterRequest duplicate = createUniqueRegisterRequest();
        duplicate.setUsername(first.getUsername());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Username already exists")));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest first = createUniqueRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = createUniqueRegisterRequest();
        duplicate.setEmail(first.getEmail());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = createUniqueRegisterRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = createUniqueRegisterRequest();
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest request = createUniqueRegisterRequest();
        request.setUsername("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_success_returnsToken() throws Exception {
        RegisterRequest reg = createUniqueRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername(reg.getUsername());
        login.setPassword(reg.getPassword());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.username").value(reg.getUsername()));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("nonexistentuser");
        login.setPassword("WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // ==================== PROTECTED ENDPOINT TESTS ====================

    @Test
    void me_withValidToken_returnsUser() throws Exception {
        // Register
        RegisterRequest reg = createUniqueRegisterRequest();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login to get token
        LoginRequest login = new LoginRequest();
        login.setUsername(reg.getUsername());
        login.setPassword(reg.getPassword());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // Access /me with token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(reg.getUsername()))
                .andExpect(jsonPath("$.email").value(reg.getEmail()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401or403() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().is(anyOf(equalTo(401), equalTo(403))));
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401or403() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().is(anyOf(equalTo(401), equalTo(403))));
    }

    // ==================== HEALTH ENDPOINT (public) ====================

    @Test
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
