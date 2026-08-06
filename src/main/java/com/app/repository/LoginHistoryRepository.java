package com.app.repository;

import com.app.entity.LoginHistoryEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    List<LoginHistoryEntity> findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    /**
     * Trims the audit trail to the configured retention window. This table takes
     * a row per sign-in attempt — successful or not — so it is the fastest
     * growing table in the schema and needs an explicit retention policy.
     */
    @Modifying
    @Query("DELETE FROM LoginHistoryEntity h WHERE h.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
