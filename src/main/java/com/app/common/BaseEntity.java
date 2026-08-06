package com.app.common;

import com.app.util.DateTimeUtil;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Mandatory audit and soft-delete columns shared by every domain table,
 * as defined by the Database Standard.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = DateTimeUtil.nowUtc();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.createdBy == null) {
            this.createdBy = ApiConstants.SYSTEM_ACTOR;
        }
        if (this.updatedBy == null) {
            this.updatedBy = ApiConstants.SYSTEM_ACTOR;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = DateTimeUtil.nowUtc();
        if (this.updatedBy == null) {
            this.updatedBy = ApiConstants.SYSTEM_ACTOR;
        }
    }

    /** Soft deletes the record; domain tables are never hard deleted. */
    public void markDeleted(String actor) {
        this.deletedAt = DateTimeUtil.nowUtc();
        this.deletedBy = actor;
        this.updatedBy = actor;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
