package com.verilens.controller;

import com.verilens.dto.AnalysisTaskResponse;
import com.verilens.dto.ErrorResponse;
import com.verilens.dto.MetadataResultResponse;
import com.verilens.dto.OcrResultResponse;
import com.verilens.entity.User;
import com.verilens.repository.UserRepository;
import com.verilens.service.AnalysisService;
import com.verilens.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analysis controller — create and query analysis tasks.
 * In Phase 2, analysis tasks are only lifecycle objects.
 * Actual AI processing is not triggered.
 */
@RestController
@RequestMapping("/api/documents/{documentId}/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AnalysisController(AnalysisService analysisService,
                              AuditService auditService,
                              UserRepository userRepository) {
        this.analysisService = analysisService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/documents/{documentId}/analysis — Start analysis.
     */
    @PostMapping
    public ResponseEntity<?> createAnalysis(
            @PathVariable UUID documentId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);

            String modules = null;
            if (body != null && body.containsKey("modules")) {
                Object modulesObj = body.get("modules");
                if (modulesObj instanceof List<?> moduleList) {
                    modules = String.join(",", moduleList.stream()
                            .map(Object::toString).toList());
                }
            }

            AnalysisTaskResponse response = analysisService.createAnalysisTask(documentId, userId, modules);

            auditService.log(userId, "ANALYSIS_INITIATED", "DOCUMENT", documentId,
                    "taskId=" + response.getJobId(), httpRequest.getRemoteAddr());

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * GET /api/documents/{documentId}/analysis/status — Check analysis status.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAnalysisStatus(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            AnalysisTaskResponse response = analysisService.getAnalysisStatus(documentId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * GET /api/documents/{documentId}/analysis — Get analysis results.
     * In Phase 2, returns only the task lifecycle data.
     */
    @GetMapping
    public ResponseEntity<?> getAnalysis(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            List<AnalysisTaskResponse> tasks = analysisService.getAnalysisTasks(documentId, userId);
            if (tasks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(404, "Not Found",
                                "No analysis found for this document",
                                httpRequest.getRequestURI()));
            }
            // Return the latest task
            return ResponseEntity.ok(tasks.get(tasks.size() - 1));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return user.getId();
    }

    /**
     * GET /api/documents/{documentId}/analysis/ocr — Retrieve OCR results.
     * Available after analysis completes (Phase 4+).
     */
    @GetMapping("/ocr")
    public ResponseEntity<?> getOcrResult(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            OcrResultResponse response = analysisService.getOcrResult(documentId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * GET /api/documents/{documentId}/analysis/metadata — Retrieve metadata analysis results.
     * Available after analysis completes (Phase 5+).
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadataResult(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            MetadataResultResponse response = analysisService.getMetadataResult(documentId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }
}
