package com.app.repository;

import com.app.entity.FileEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByPublicIdAndDeletedAtIsNull(UUID publicId);
    List<FileEntity> findAllByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileEntity f WHERE f.deletedAt IS NULL")
    long calculateTotalStorageUsed();
    
    long countByDeletedAtIsNull();
}
