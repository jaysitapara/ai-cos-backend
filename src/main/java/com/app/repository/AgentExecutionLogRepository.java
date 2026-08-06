package com.app.repository;

import com.app.entity.AgentExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLogEntity, Long> {
    List<AgentExecutionLogEntity> findBySessionIdOrderByTimestampAsc(Long sessionId);
}
