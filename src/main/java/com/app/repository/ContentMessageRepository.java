package com.app.repository;

import com.app.entity.ContentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentMessageRepository extends JpaRepository<ContentMessageEntity, Long> {

    List<ContentMessageEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);
}
