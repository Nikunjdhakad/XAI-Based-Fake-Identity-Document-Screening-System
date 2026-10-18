package com.verilens.controller;

import com.verilens.config.AiServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for the backend service.
 * Reports status of the backend itself, the database connection,
 * and the AI service reachability.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final AiServiceConfig aiServiceConfig;
    private final RestTemplate restTemplate;

    public HealthController(DataSource dataSource, AiServiceConfig aiServiceConfig) {
        this.dataSource = dataSource;
        this.aiServiceConfig = aiServiceConfig;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "verilens-backend");
        response.put("timestamp", Instant.now().toString());
        response.put("database", checkDatabase());
        response.put("aiService", checkAiService());

        return ResponseEntity.ok(response);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkAiService() {
        try {
            String url = aiServiceConfig.getUrl() + "/internal/health";
            ResponseEntity<String> aiResponse = restTemplate.getForEntity(url, String.class);
            if (aiResponse.getStatusCode().is2xxSuccessful()) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            log.debug("AI service health check failed: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
}
