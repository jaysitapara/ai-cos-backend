package com.app.repository;

import com.app.entity.ProjectQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectQuestionRepository extends JpaRepository<ProjectQuestionEntity, Long> {
    List<ProjectQuestionEntity> findBySessionIdOrderByOrderIndexAsc(Long sessionId);
    Optional<ProjectQuestionEntity> findBySessionIdAndQuestionKey(Long sessionId, String questionKey);
}
