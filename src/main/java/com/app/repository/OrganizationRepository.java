package com.app.repository;

import com.app.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {
    Optional<OrganizationEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<OrganizationEntity> findAllByDeletedAtIsNullOrderByNameAsc();
    long countByDeletedAtIsNull();
}
