package com.app.repository;

import com.app.entity.ContentFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentFeedbackRepository extends JpaRepository<ContentFeedbackEntity, Long> {

    List<ContentFeedbackEntity> findByContentIdOrderByCreatedAtDesc(Long contentId);
}
