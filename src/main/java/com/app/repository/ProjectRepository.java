package com.app.repository;

import com.app.entity.ProjectEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<ProjectEntity> findAllByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);
    List<ProjectEntity> findAllByDeletedAtIsNull();
    long countByDeletedAtIsNull();
}
