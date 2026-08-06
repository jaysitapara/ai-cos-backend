package com.app.repository;

import com.app.entity.TaskEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Optional<TaskEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<TaskEntity> findAllByAssigneeAndDeletedAtIsNullOrderByDueDateAsc(UserEntity assignee, Pageable pageable);
    List<TaskEntity> findAllByDeletedAtIsNullOrderByDueDateAsc(Pageable pageable);
    List<TaskEntity> findAllByDeletedAtIsNull();
    long countByDeletedAtIsNull();
    long countByStatusAndDeletedAtIsNull(String status);
}
