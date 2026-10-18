package com.verilens.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("verilens-backend"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.aiService").exists());
    }

    @Test
    void healthEndpointIsPublic_noAuthRequired() throws Exception {
        // Health endpoint must be accessible without JWT
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}
