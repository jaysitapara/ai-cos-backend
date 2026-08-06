package com.app.service;

import com.app.repository.RefreshTokenRepository;
import com.app.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tears down every session of an account from a transaction of its own.
 *
 * <p>Needed for the refresh-token reuse path specifically: that flow revokes the
 * account's sessions and then rejects the request by throwing, which rolls the
 * caller back. Revoking inline would be undone by that rollback, leaving a
 * detected compromise with all its sessions still live — the containment step
 * has to commit independently of the rejection that triggers it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(Long userId, String reason) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId, DateTimeUtil.nowUtc(), reason);
        log.info("Revoked {} sessions for user id={} reason={}", revoked, userId, reason);
    }
}
