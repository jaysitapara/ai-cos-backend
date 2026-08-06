package com.app.repository;

import com.app.entity.OrganizationEntity;
import com.app.entity.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {
    Optional<WorkspaceEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<WorkspaceEntity> findAllByOrganizationAndDeletedAtIsNullOrderByNameAsc(OrganizationEntity organization);
    List<WorkspaceEntity> findAllByDeletedAtIsNullOrderByNameAsc();
}
