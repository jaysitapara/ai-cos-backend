package com.app.repository;

import com.app.entity.ContentBriefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentBriefRepository extends JpaRepository<ContentBriefEntity, Long> {

    Optional<ContentBriefEntity> findByPublicId(UUID publicId);

    Optional<ContentBriefEntity> findFirstByThreadIdOrderByCreatedAtDesc(Long threadId);
}
