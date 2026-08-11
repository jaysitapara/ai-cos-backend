package com.app.repository;

import com.app.entity.BrandProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandProductRepository extends JpaRepository<BrandProductEntity, Long> {

    List<BrandProductEntity> findByBrandIdAndUserIdOrderByCreatedAtDesc(Long brandId, Long userId);

    Optional<BrandProductEntity> findByPublicIdAndUserId(UUID publicId, Long userId);
}
