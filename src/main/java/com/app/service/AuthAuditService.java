package com.app.service;

import com.app.config.AuthProperties;
import com.app.entity.LoginHistoryEntity;
import com.app.entity.UserEntity;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.UserRepository;
import com.app.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Durable side effects of an authentication attempt: the audit trail and the
 * brute-force lockout counter.
 *
 * <p>The propagation on each method is load-bearing.
 *
 * <p><b>Failures</b> commit in their own transaction ({@link Propagation#REQUIRES_NEW}).
 * A rejected sign-in reports itself by throwing, which rolls the calling
 * transaction back — writing the audit row or incrementing the failure counter
 * inline would silently discard both. Before this split the lockout counter
 * never advanced past its first increment and no failed attempt was ever
 * recorded, so the account lockout could not trigger at all.
 *
 * <p><b>Successes</b> deliberately join the caller instead. The account they
 * reference may have been inserted moments earlier by that same uncommitted
 * transaction (registration), and a separate transaction cannot see it — the
 * foreign key would fail. Joining also gives the right semantics: if the
 * sign-in rolls back, the row claiming it succeeded should go with it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditService {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    /** user_agent is a 500 character column and browsers happily send more. */
    private static final int USER_AGENT_MAX_LENGTH = 500;

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;

    /** Records a successful attempt inside the caller's transaction. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordSuccess(UserEntity user, String email, String detail, String ipAddress, String userAgent) {
        save(user, email, STATUS_SUCCESS, detail, ipAddress, userAgent);
    }

    /**
     * Records a rejected attempt in an independent transaction so it outlives the
     * caller's rollback.
     *
     * @param userId the account involved, or {@code null} when the email is unknown
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId, String email, String reason, String ipAddress, String userAgent) {
        try {
            // A reference, not a fetch: only the foreign key value is needed, and
            // the row is already committed by the time any failure is recorded.
            UserEntity user = userId != null ? userRepository.getReferenceById(userId) : null;
            save(user, email, STATUS_FAILED, reason, ipAddress, userAgent);
        } catch (RuntimeException ex) {
            log.error("Unable to persist login audit entry for email={}", email, ex);
        }
    }

    /** Increments the failure counter and locks the account once the threshold is reached. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(Long userId) {
        OffsetDateTime now = DateTimeUtil.nowUtc();
        try {
            userRepository.registerFailedLoginAttempt(
                userId,
                authProperties.maxFailedLoginAttempts(),
                now.plusMinutes(authProperties.lockDurationMinutes()),
                now);
        } catch (RuntimeException ex) {
            log.error("Unable to record failed login attempt for user id={}", userId, ex);
        }
    }

    private void save(UserEntity user, String email, String status, String detail, String ipAddress, String userAgent) {
        loginHistoryRepository.save(LoginHistoryEntity.builder()
            .publicId(UUID.randomUUID())
            .user(user)
            .email(email)
            .status(status)
            .failureReason(detail)
            .ipAddress(ipAddress)
            .userAgent(truncateUserAgent(userAgent))
            .build());
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= USER_AGENT_MAX_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, USER_AGENT_MAX_LENGTH);
    }
}
