package com.app.repository;

import com.app.entity.ContentPerformanceSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentPerformanceSnapshotRepository extends JpaRepository<ContentPerformanceSnapshotEntity, Long> {
    List<ContentPerformanceSnapshotEntity> findByContentAnalyticsIdOrderBySnapshotAtDesc(Long contentAnalyticsId);
}
