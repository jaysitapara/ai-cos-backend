package com.app.repository;

import com.app.entity.ContentQuestionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentQuestionStateRepository extends JpaRepository<ContentQuestionStateEntity, Long> {

    List<ContentQuestionStateEntity> findByThreadIdOrderByOrderIndexAsc(Long threadId);

    Optional<ContentQuestionStateEntity> findByThreadIdAndQuestionKey(Long threadId, String questionKey);

    void deleteByThreadId(Long threadId);
}
