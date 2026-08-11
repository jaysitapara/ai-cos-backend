package com.app.repository;

import com.app.entity.BrandKnowledgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandKnowledgeRepository extends JpaRepository<BrandKnowledgeEntity, Long> {

    List<BrandKnowledgeEntity> findByBrandIdAndUserIdAndStatusOrderByUpdatedAtDesc(Long brandId, Long userId, String status);

    List<BrandKnowledgeEntity> findByBrandIdAndUserIdOrderByUpdatedAtDesc(Long brandId, Long userId);

    Optional<BrandKnowledgeEntity> findByPublicIdAndUserId(UUID publicId, Long userId);
}
