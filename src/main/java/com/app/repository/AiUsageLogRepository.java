package com.app.repository;

import com.app.entity.AiUsageLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLogEntity, Long> {
    Optional<AiUsageLogEntity> findByPublicId(UUID publicId);
    Page<AiUsageLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AiUsageLogEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
}
