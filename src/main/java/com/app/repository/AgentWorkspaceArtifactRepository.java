package com.app.repository;

import com.app.entity.AgentWorkspaceArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentWorkspaceArtifactRepository extends JpaRepository<AgentWorkspaceArtifactEntity, Long> {
    List<AgentWorkspaceArtifactEntity> findBySessionIdOrderByFilePathAsc(Long sessionId);
    Optional<AgentWorkspaceArtifactEntity> findBySessionIdAndFilePath(Long sessionId, String filePath);
}
