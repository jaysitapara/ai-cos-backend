package com.app.repository;

import com.app.entity.ProjectCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectCommentEntity, Long> {
    Optional<ProjectCommentEntity> findByPublicId(UUID publicId);
    List<ProjectCommentEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Page<ProjectCommentEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
    List<ProjectCommentEntity> findByProjectIdAndVersionIdOrderByCreatedAtDesc(Long projectId, Long versionId);
}
