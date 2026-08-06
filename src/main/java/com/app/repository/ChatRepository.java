package com.app.repository;

import com.app.entity.ChatEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
    Optional<ChatEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<ChatEntity> findAllByUserAndDeletedAtIsNullOrderByUpdatedAtDesc(UserEntity user, Pageable pageable);
    List<ChatEntity> findAllByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);
    long countByDeletedAtIsNull();
}
