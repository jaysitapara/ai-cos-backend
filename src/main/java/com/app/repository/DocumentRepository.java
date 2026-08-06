package com.app.repository;

import com.app.entity.DocumentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<DocumentEntity> findAllByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);
    long countByDeletedAtIsNull();
}
