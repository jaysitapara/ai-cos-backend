package com.app.repository;

import com.app.entity.ProjectVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectVersionRepository extends JpaRepository<ProjectVersionEntity, Long> {
    Optional<ProjectVersionEntity> findByPublicId(UUID publicId);
    List<ProjectVersionEntity> findByProjectIdOrderByVersionNumberDesc(Long projectId);
    Optional<ProjectVersionEntity> findFirstByProjectIdOrderByVersionNumberDesc(Long projectId);
    Optional<ProjectVersionEntity> findByProjectIdAndVersionNumber(Long projectId, Integer versionNumber);
}
