package com.app.repository;

import com.app.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByVerificationTokenAndDeletedAtIsNull(String verificationToken);

    Optional<UserEntity> findByPasswordResetTokenAndDeletedAtIsNull(String passwordResetToken);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<UserEntity> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
