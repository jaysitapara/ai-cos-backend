package com.app.repository;

import com.app.entity.ActivityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {
    Optional<ActivityEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<ActivityEntity> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
}
