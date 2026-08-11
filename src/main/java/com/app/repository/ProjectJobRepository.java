package com.app.repository;

import com.app.entity.ProjectJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectJobRepository extends JpaRepository<ProjectJobEntity, Long> {
    Optional<ProjectJobEntity> findByPublicId(UUID publicId);
    List<ProjectJobEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<ProjectJobEntity> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<ProjectJobEntity> findByProjectIdAndStatus(Long projectId, String status);
}
