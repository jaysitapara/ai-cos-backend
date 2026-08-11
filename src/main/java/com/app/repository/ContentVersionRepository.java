package com.app.repository;

import com.app.entity.ContentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersionEntity, Long> {

    List<ContentVersionEntity> findByContentIdOrderByVersionNumberDesc(Long contentId);

    Optional<ContentVersionEntity> findByPublicId(UUID publicId);

    Optional<ContentVersionEntity> findFirstByContentIdOrderByVersionNumberDesc(Long contentId);
}
