package com.app.repository;

import com.app.entity.ContentIntelligenceInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentIntelligenceInsightRepository extends JpaRepository<ContentIntelligenceInsightEntity, Long> {
    List<ContentIntelligenceInsightEntity> findByBrandIdAndUserIdAndStatus(Long brandId, Long userId, String status);
    List<ContentIntelligenceInsightEntity> findByUserIdAndStatus(Long userId, String status);
    Optional<ContentIntelligenceInsightEntity> findByBrandIdAndUserIdAndInsightKey(Long brandId, Long userId, String insightKey);
    Optional<ContentIntelligenceInsightEntity> findByPublicIdAndUserId(UUID publicId, Long userId);
}
