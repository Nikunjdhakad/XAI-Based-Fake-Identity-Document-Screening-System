package com.verilens.service;

import com.verilens.dto.AnalysisTaskResponse;
import com.verilens.entity.*;
import com.verilens.repository.AnalysisTaskRepository;
import com.verilens.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Analysis service — manages analysis task lifecycle.
 * In Phase 2, this only creates and queries tasks.
 * Actual AI processing will be connected in Phases 4–9.
 */
@Service
public class AnalysisService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final DocumentRepository documentRepository;

    public AnalysisService(AnalysisTaskRepository analysisTaskRepository,
                           DocumentRepository documentRepository) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Create a new analysis task for a document.
     * Sets the document status to QUEUED.
     * Does NOT trigger actual AI processing — that comes in later phases.
     */
    @Transactional
    public AnalysisTaskResponse createAnalysisTask(UUID documentId, UUID userId, String modules) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        // Update document status
        document.setStatus(DocumentStatus.QUEUED);
        documentRepository.save(document);

        // Create analysis task
        AnalysisTask task = new AnalysisTask();
        task.setDocument(document);
        task.setStatus(AnalysisTaskStatus.QUEUED);
        task.setRequestedModules(modules);

        AnalysisTask savedTask = analysisTaskRepository.save(task);
        return AnalysisTaskResponse.fromEntity(savedTask);
    }

    /**
     * Get the latest analysis task status for a document.
     */
    @Transactional(readOnly = true)
    public AnalysisTaskResponse getAnalysisStatus(UUID documentId, UUID userId) {
        // Verify document ownership
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        AnalysisTask task = analysisTaskRepository.findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No analysis task found for this document"));

        return AnalysisTaskResponse.fromEntity(task);
    }

    /**
     * Get all analysis tasks for a document.
     */
    @Transactional(readOnly = true)
    public List<AnalysisTaskResponse> getAnalysisTasks(UUID documentId, UUID userId) {
        // Verify document ownership
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        return analysisTaskRepository.findByDocumentId(documentId)
                .stream()
                .map(AnalysisTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
