package com.app.repository;

import com.app.entity.ApprovedContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovedContentRepository extends JpaRepository<ApprovedContentEntity, Long> {

    List<ApprovedContentEntity> findByBrandIdAndUserIdAndContentTypeOrderByCreatedAtDesc(Long brandId, Long userId, String contentType);

    List<ApprovedContentEntity> findByBrandIdAndUserIdOrderByCreatedAtDesc(Long brandId, Long userId);
}
