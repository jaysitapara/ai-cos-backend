package com.app.repository;

import com.app.entity.ProjectFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFileEntity, Long> {
    List<ProjectFileEntity> findByProjectIdAndIsDeletedFalseOrderByFilePathAsc(Long projectId);
    List<ProjectFileEntity> findByProjectIdAndVersionIdAndIsDeletedFalseOrderByFilePathAsc(Long projectId, Long versionId);
    Optional<ProjectFileEntity> findByProjectIdAndFilePathAndIsDeletedFalse(Long projectId, String filePath);
}
