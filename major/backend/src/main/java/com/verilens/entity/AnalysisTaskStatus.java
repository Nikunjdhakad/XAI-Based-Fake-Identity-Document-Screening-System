package com.verilens.entity;

/**
 * Analysis task status lifecycle.
 * QUEUED → PROCESSING → COMPLETED | FAILED | PARTIALLY_COMPLETED
 */
public enum AnalysisTaskStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}
