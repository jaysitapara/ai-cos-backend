package com.app.repository;

import com.app.entity.NotificationEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Optional<NotificationEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<NotificationEntity> findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(UserEntity user, Pageable pageable);
    List<NotificationEntity> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
    long countByUserAndIsReadFalseAndDeletedAtIsNull(UserEntity user);
    long countByIsReadFalseAndDeletedAtIsNull();
}
