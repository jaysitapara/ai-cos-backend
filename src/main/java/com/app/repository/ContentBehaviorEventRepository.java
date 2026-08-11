package com.app.repository;

import com.app.entity.ContentBehaviorEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentBehaviorEventRepository extends JpaRepository<ContentBehaviorEventEntity, Long> {
    List<ContentBehaviorEventEntity> findByBrandIdAndUserIdOrderByCreatedAtDesc(Long brandId, Long userId);
    List<ContentBehaviorEventEntity> findByBrandIdAndUserIdAndEventType(Long brandId, Long userId, String eventType);
    long countByBrandIdAndUserIdAndEventType(Long brandId, Long userId, String eventType);
}
