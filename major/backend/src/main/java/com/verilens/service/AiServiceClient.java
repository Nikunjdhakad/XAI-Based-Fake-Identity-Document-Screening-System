package com.verilens.service;

import com.verilens.config.AiServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for the internal Python AI service.
 * Called by AnalysisService to dispatch document analysis work.
 * The frontend never calls this — it is internal only.
 */
@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final AiServiceConfig config;
    private final RestTemplate restTemplate;

    public AiServiceClient(AiServiceConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send a document to the Python AI service for analysis.
     *
     * @param documentId  Document UUID string (for AI service logging/tracking)
     * @param fileBytes   Raw file bytes
     * @param filename    Original filename (used in Content-Disposition)
     * @param contentType MIME type of the file
     * @param modules     Comma-separated module list (e.g. "OCR")
     * @return Parsed response body as a Map, or null if the call failed
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Object> analyze(
            String documentId,
            byte[] fileBytes,
            String filename,
            String contentType,
            String modules) {

        String url = config.getUrl() + "/internal/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", config.getApiKey());

        // Wrap the file bytes in a named resource
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() { return filename; }
        };

        HttpHeaders filePart = new HttpHeaders();
        filePart.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : "application/octet-stream"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, filePart));
        body.add("documentId", documentId);
        body.add("modules", modules != null ? modules : "OCR");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            log.error("AI service returned non-2xx: {} for document {}", response.getStatusCode(), documentId);
            return null;
        } catch (Exception e) {
            log.error("Failed to call AI service at {} for document {}: {}", url, documentId, e.getMessage());
            return null;
        }
    }
}
