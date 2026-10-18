package com.verilens.controller;

import com.verilens.dto.DocumentResponse;
import com.verilens.dto.ErrorResponse;
import com.verilens.entity.DocumentStatus;
import com.verilens.entity.User;
import com.verilens.repository.UserRepository;
import com.verilens.service.AuditService;
import com.verilens.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Document controller — upload, list, detail, and delete documents.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              AuditService auditService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/documents — Upload a document.
     */
    @PostMapping
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "referenceFace", required = false) MultipartFile referenceFace,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            DocumentResponse response = documentService.uploadDocument(
                    userId, file, documentType, description, referenceFace);

            auditService.log(userId, "DOCUMENT_UPLOADED", "DOCUMENT", response.getId(),
                    "type=" + documentType, httpRequest.getRemoteAddr());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(400, "Bad Request", e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal Server Error",
                            "Upload failed", httpRequest.getRequestURI()));
        }
    }

    /**
     * GET /api/documents — List documents for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<?> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = resolveUserId(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));

        DocumentStatus docStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                docStatus = DocumentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status filter — ignore and return all
            }
        }

        Page<DocumentResponse> documents = documentService.getDocuments(userId, docStatus, pageable);
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/{id} — Get document details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            DocumentResponse response = documentService.getDocument(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", "Document not found",
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * DELETE /api/documents/{id} — Delete a document.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        try {
            UUID userId = resolveUserId(userDetails);
            documentService.deleteDocument(id, userId);

            auditService.log(userId, "DOCUMENT_DELETED", "DOCUMENT", id,
                    null, httpRequest.getRemoteAddr());

            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Not Found", "Document not found",
                            httpRequest.getRequestURI()));
        }
    }

    /**
     * Resolve the authenticated user's UUID from UserDetails.
     */
    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return user.getId();
    }
}
