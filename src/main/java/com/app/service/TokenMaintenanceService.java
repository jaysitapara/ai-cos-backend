package com.app.service;

import com.app.config.AuthProperties;
import com.app.repository.LoginHistoryRepository;
import com.app.repository.RefreshTokenRepository;
import com.app.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Retention sweep for the two tables that grow with traffic rather than with the
 * number of accounts.
 *
 * <p>At a hundred thousand users, refresh-token rotation writes a row per token
 * refresh per device and login history writes a row per attempt. Left alone,
 * both tables outgrow their indexes' ability to stay cached and drag down the
 * sign-in path they exist to support. Rows are only removed once they can no
 * longer authenticate anyone or serve the audit window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenMaintenanceService {

    /**
     * Revoked and expired tokens are kept this long past their end so the reuse
     * detector still has something to match a replayed token against.
     */
    private static final int REVOKED_TOKEN_GRACE_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final AuthProperties authProperties;

    @Scheduled(cron = "${app.auth.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeStaleAuthRecords() {
        OffsetDateTime now = DateTimeUtil.nowUtc();

        int tokens = refreshTokenRepository.deleteExpiredAndRevokedBefore(now.minusDays(REVOKED_TOKEN_GRACE_DAYS));
        int history = loginHistoryRepository.deleteOlderThan(now.minusDays(authProperties.loginHistoryRetentionDays()));

        if (tokens > 0 || history > 0) {
            log.info("Auth retention sweep removed {} refresh tokens and {} login history rows", tokens, history);
        }
    }
}
