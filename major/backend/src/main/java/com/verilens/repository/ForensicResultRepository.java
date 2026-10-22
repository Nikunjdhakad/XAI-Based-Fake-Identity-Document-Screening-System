package com.verilens.repository;

import com.verilens.entity.ForensicResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForensicResultRepository extends JpaRepository<ForensicResult, UUID> {
    Optional<ForensicResult> findByTaskId(UUID taskId);
    Optional<ForensicResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
