package com.app.repository;

import com.app.entity.MeetingEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {
    Optional<MeetingEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<MeetingEntity> findAllByUserAndStartTimeAfterAndDeletedAtIsNullOrderByStartTimeAsc(UserEntity user, OffsetDateTime now, Pageable pageable);
    List<MeetingEntity> findAllByStartTimeAfterAndDeletedAtIsNullOrderByStartTimeAsc(OffsetDateTime now, Pageable pageable);
    long countByDeletedAtIsNull();
}
