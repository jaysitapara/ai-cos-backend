package com.app.repository;

import com.app.entity.GeneratedContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedContentRepository extends JpaRepository<GeneratedContentEntity, Long> {

    List<GeneratedContentEntity> findByUserIdAndBrandIdOrderByUpdatedAtDesc(Long userId, Long brandId);

    Optional<GeneratedContentEntity> findByPublicIdAndUserId(UUID publicId, Long userId);

    Optional<GeneratedContentEntity> findFirstByThreadIdOrderByCreatedAtDesc(Long threadId);
}
