package com.app.repository;

import com.app.entity.AgentExecutionTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentExecutionTaskRepository extends JpaRepository<AgentExecutionTaskEntity, Long> {
    List<AgentExecutionTaskEntity> findBySessionIdOrderByExecutionOrderAsc(Long sessionId);
    List<AgentExecutionTaskEntity> findBySessionIdAndStatus(Long sessionId, String status);
    void deleteBySessionId(Long sessionId);
}
