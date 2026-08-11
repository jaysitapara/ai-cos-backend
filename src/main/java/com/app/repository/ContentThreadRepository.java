package com.app.repository;

import com.app.entity.ContentThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentThreadRepository extends JpaRepository<ContentThreadEntity, Long> {

    List<ContentThreadEntity> findByUserIdAndBrandIdOrderByUpdatedAtDesc(Long userId, Long brandId);

    Optional<ContentThreadEntity> findByPublicIdAndUserId(UUID publicId, Long userId);
}
