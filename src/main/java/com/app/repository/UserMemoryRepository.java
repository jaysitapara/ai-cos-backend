package com.app.repository;

import com.app.entity.UserMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemoryEntity, Long> {

    List<UserMemoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
