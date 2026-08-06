package com.app.repository;

import com.app.entity.RefreshTokenEntity;
import com.app.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Looks a session up by the SHA-256 digest of the presented token. The user is
     * fetched in the same statement because every caller immediately needs it —
     * without the join this is an N+1 on the hottest authenticated path.
     */
    @Query("SELECT t FROM RefreshTokenEntity t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash AND t.deletedAt IS NULL")
    Optional<RefreshTokenEntity> findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<RefreshTokenEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    List<RefreshTokenEntity> findAllByUserAndIsRevokedFalseAndDeletedAtIsNullOrderByLastAccessedAtDesc(UserEntity user);

    /**
     * Revokes every live session of a user in one statement. The previous
     * load-then-save-each approach pulled the whole session set into the
     * persistence context, which does not hold up for accounts with many devices.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshTokenEntity t
           SET t.isRevoked = true,
               t.revokedAt = :revokedAt,
               t.revokedReason = :reason,
               t.updatedAt = :revokedAt
         WHERE t.user.id = :userId
           AND t.isRevoked = false
           AND t.deletedAt IS NULL
        """)
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("revokedAt") OffsetDateTime revokedAt,
                         @Param("reason") String reason);

    /**
     * Purges sessions that can no longer authenticate anyone. Refresh tokens are
     * write-heavy and short-lived; without this sweep the table grows without
     * bound and its indexes stop fitting in cache.
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :cutoff OR (t.isRevoked = true AND t.revokedAt < :cutoff)")
    int deleteExpiredAndRevokedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
