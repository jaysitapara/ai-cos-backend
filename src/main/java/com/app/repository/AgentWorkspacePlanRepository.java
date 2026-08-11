package com.app.repository;

import com.app.entity.AgentWorkspacePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentWorkspacePlanRepository extends JpaRepository<AgentWorkspacePlanEntity, Long> {
    Optional<AgentWorkspacePlanEntity> findByPublicId(UUID publicId);
    Optional<AgentWorkspacePlanEntity> findBySessionId(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
