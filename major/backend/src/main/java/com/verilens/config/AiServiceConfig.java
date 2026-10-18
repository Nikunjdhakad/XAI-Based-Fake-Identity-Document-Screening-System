package com.verilens.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Centralizes AI service connection configuration.
 * Values are injected from application.yml / environment variables.
 */
@Configuration
public class AiServiceConfig {

    @Value("${verilens.ai-service.url}")
    private String url;

    @Value("${verilens.ai-service.api-key}")
    private String apiKey;

    @Value("${verilens.ai-service.timeout-seconds:120}")
    private int timeoutSeconds;

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
