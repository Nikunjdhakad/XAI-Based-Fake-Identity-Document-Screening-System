package com.verilens.repository;

import com.verilens.entity.AnalysisTask;
import com.verilens.entity.AnalysisTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, UUID> {
    List<AnalysisTask> findByDocumentId(UUID documentId);
    Optional<AnalysisTask> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    List<AnalysisTask> findByStatus(AnalysisTaskStatus status);
}
