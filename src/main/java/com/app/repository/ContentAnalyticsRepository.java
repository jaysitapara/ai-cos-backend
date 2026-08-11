package com.app.repository;

import com.app.entity.ContentAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentAnalyticsRepository extends JpaRepository<ContentAnalyticsEntity, Long> {
    Optional<ContentAnalyticsEntity> findByPublicIdAndUserId(UUID publicId, Long userId);
    Optional<ContentAnalyticsEntity> findByContentIdAndUserId(Long contentId, Long userId);
    List<ContentAnalyticsEntity> findByBrandIdAndUserIdOrderByCreatedAtDesc(Long brandId, Long userId);
    List<ContentAnalyticsEntity> findByBrandIdAndUserIdAndStatus(Long brandId, Long userId, String status);
    List<ContentAnalyticsEntity> findByBrandIdAndUserIdAndContentType(Long brandId, Long userId, String contentType);
}
