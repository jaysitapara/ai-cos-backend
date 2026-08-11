package com.app.repository;

import com.app.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<BrandEntity> findByPublicIdAndUserId(UUID publicId, Long userId);

    Optional<BrandEntity> findByIdAndUserId(Long id, Long userId);
}
