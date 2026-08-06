package com.app.entity;

import com.app.common.BaseEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** SHA-256 digest of the emailed verification token; the plaintext is never stored. */
    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_expires_at")
    private OffsetDateTime verificationExpiresAt;

    /** SHA-256 digest of the emailed password reset token; the plaintext is never stored. */
    @Column(name = "password_reset_token_hash", length = 64)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private OffsetDateTime passwordResetExpiresAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "current_organization_id")
    private UUID currentOrganizationId;

    /**
     * Set when the account was created through an identity provider and has no
     * password of its own. Such accounts must not be able to sign in with the
     * random placeholder hash they were seeded with.
     */
    @Column(name = "password_login_enabled", nullable = false)
    private boolean passwordLoginEnabled;

    @PrePersist
    void applyDefaults() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.role == null) {
            this.role = UserRole.ROLE_USER;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public boolean isAccountNonLocked(OffsetDateTime now) {
        return this.lockedUntil == null || this.lockedUntil.isBefore(now);
    }
}
