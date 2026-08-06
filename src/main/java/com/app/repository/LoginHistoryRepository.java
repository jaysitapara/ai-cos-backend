package com.app.repository;

import com.app.entity.LoginHistoryEntity;
import com.app.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    List<LoginHistoryEntity> findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    List<LoginHistoryEntity> findAllByEmailAndDeletedAtIsNullOrderByCreatedAtDesc(String email, Pageable pageable);
}
