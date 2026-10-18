package com.verilens.entity;

/**
 * Document status lifecycle.
 * UPLOADED → QUEUED → PROCESSING → COMPLETED | FAILED | PARTIALLY_COMPLETED
 */
public enum DocumentStatus {
    UPLOADED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}
