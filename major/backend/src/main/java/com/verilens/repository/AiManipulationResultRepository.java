package com.verilens.repository;

import com.verilens.entity.AiManipulationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AI Manipulation screening results — Phase 9.
 */
@Repository
public interface AiManipulationResultRepository extends JpaRepository<AiManipulationResult, UUID> {

    Optional<AiManipulationResult> findByTaskId(UUID taskId);

    Optional<AiManipulationResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
