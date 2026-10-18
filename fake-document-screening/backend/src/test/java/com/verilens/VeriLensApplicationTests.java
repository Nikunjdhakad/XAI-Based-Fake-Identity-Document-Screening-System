package com.verilens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VeriLensApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts without errors
        // including all Phase 2 entities, repositories, services, and security config.
    }
}
