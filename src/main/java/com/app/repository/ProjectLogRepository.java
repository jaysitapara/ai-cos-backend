package com.app.repository;

import com.app.entity.ProjectLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLogRepository extends JpaRepository<ProjectLogEntity, Long> {
    List<ProjectLogEntity> findByProjectIdOrderByTimestampDesc(Long projectId);
    Page<ProjectLogEntity> findByProjectIdOrderByTimestampDesc(Long projectId, Pageable pageable);
    List<ProjectLogEntity> findByJobIdOrderByTimestampAsc(Long jobId);
}
