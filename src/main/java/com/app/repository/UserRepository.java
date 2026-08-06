package com.app.repository;

import com.app.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByVerificationTokenHashAndDeletedAtIsNull(String verificationTokenHash);

    Optional<UserEntity> findByPasswordResetTokenHashAndDeletedAtIsNull(String passwordResetTokenHash);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * Records a failed credential attempt and applies the lockout in one
     * statement, outside the caller's transaction.
     *
     * <p>This has to be a direct update rather than a dirty-checked entity write:
     * the login flow rejects the attempt by throwing, which rolls its transaction
     * back. An entity mutation would be discarded along with it and the lockout
     * counter would never advance — see {@code AuthAuditService}.
     */
    @Modifying
    @Query("""
        UPDATE UserEntity u
           SET u.failedLoginAttempts = u.failedLoginAttempts + 1,
               u.lockedUntil = CASE WHEN u.failedLoginAttempts + 1 >= :maxAttempts THEN :lockUntil ELSE u.lockedUntil END,
               u.updatedAt = :now
         WHERE u.id = :userId
        """)
    int registerFailedLoginAttempt(@Param("userId") Long userId,
                                   @Param("maxAttempts") int maxAttempts,
                                   @Param("lockUntil") OffsetDateTime lockUntil,
                                   @Param("now") OffsetDateTime now);
}
