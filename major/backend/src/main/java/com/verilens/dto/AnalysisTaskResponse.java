package com.verilens.dto;

import com.verilens.entity.AnalysisTask;

import java.time.Instant;
import java.util.UUID;

/**
 * Analysis task response DTO.
 */
public class AnalysisTaskResponse {

    private UUID jobId;
    private UUID documentId;
    private String status;
    private String requestedModules;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;

    public AnalysisTaskResponse() {}

    public static AnalysisTaskResponse fromEntity(AnalysisTask task) {
        AnalysisTaskResponse dto = new AnalysisTaskResponse();
        dto.setJobId(task.getId());
        dto.setDocumentId(task.getDocument().getId());
        dto.setStatus(task.getStatus().name());
        dto.setRequestedModules(task.getRequestedModules());
        dto.setRequestedAt(task.getCreatedAt());
        dto.setStartedAt(task.getStartedAt());
        dto.setCompletedAt(task.getCompletedAt());
        return dto;
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedModules() { return requestedModules; }
    public void setRequestedModules(String requestedModules) { this.requestedModules = requestedModules; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
