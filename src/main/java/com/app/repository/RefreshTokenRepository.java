package com.app.repository;

import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHashAndDeletedAtIsNull(String tokenHash);

    Optional<RefreshTokenEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    List<RefreshTokenEntity> findAllByUserAndIsRevokedFalseAndDeletedAtIsNullOrderByLastAccessedAtDesc(UserEntity user);

    List<RefreshTokenEntity> findAllByUser(UserEntity user);

    void deleteAllByUser(UserEntity user);
}
