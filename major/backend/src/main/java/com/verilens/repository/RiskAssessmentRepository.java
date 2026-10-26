package com.verilens.repository;

import com.verilens.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
    Optional<RiskAssessment> findByTaskId(UUID taskId);
    Optional<RiskAssessment> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
