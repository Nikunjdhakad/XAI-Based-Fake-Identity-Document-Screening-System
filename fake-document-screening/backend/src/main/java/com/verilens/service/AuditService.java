package com.verilens.service;

import com.verilens.entity.AuditLog;
import com.verilens.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Audit service — records security-relevant actions to the audit_logs table.
 * Does not log sensitive information (passwords, tokens).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an audit event.
     *
     * @param userId     User performing the action (nullable for failed logins)
     * @param action     Action identifier (e.g., USER_REGISTERED, LOGIN_SUCCESS)
     * @param entityType Type of entity affected (e.g., USER, DOCUMENT)
     * @param entityId   ID of the entity affected (nullable)
     * @param details    Additional context (no sensitive data)
     * @param ipAddress  Client IP address
     */
    public void log(UUID userId, String action, String entityType, UUID entityId,
                    String details, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            entry.setIpAddress(ipAddress);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must not break the main flow
            log.error("Failed to write audit log: action={}, error={}", action, e.getMessage());
        }
    }

    /**
     * Convenience method when no entity is affected.
     */
    public void log(UUID userId, String action, String ipAddress) {
        log(userId, action, null, null, null, ipAddress);
    }
}
