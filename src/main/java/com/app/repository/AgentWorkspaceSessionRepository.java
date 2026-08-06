package com.app.repository;

import com.app.entity.AgentWorkspaceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentWorkspaceSessionRepository extends JpaRepository<AgentWorkspaceSessionEntity, Long> {
    Optional<AgentWorkspaceSessionEntity> findByPublicId(UUID publicId);
    List<AgentWorkspaceSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AgentWorkspaceSessionEntity> findAllByOrderByCreatedAtDesc();
}
