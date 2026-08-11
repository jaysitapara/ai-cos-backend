package com.app.repository;

import com.app.entity.ProjectCreationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectCreationSessionRepository extends JpaRepository<ProjectCreationSessionEntity, Long> {

    Optional<ProjectCreationSessionEntity> findByPublicId(UUID publicId);

    Optional<ProjectCreationSessionEntity> findByPublicIdAndUserId(UUID publicId, Long userId);

    Optional<ProjectCreationSessionEntity> findByProjectId(Long projectId);

    /**
     * Eagerly fetches the questions collection in a single JOIN query.
     * Use this variant whenever the caller needs to serialize questions
     * (e.g., the controller's mapSession() method) so no lazy-load occurs
     * after the JPA session has closed.
     */
    @Query("SELECT DISTINCT s FROM ProjectCreationSessionEntity s LEFT JOIN FETCH s.user u LEFT JOIN FETCH s.questions q WHERE s.publicId = :publicId")
    Optional<ProjectCreationSessionEntity> findByPublicIdWithQuestions(@Param("publicId") UUID publicId);
}
