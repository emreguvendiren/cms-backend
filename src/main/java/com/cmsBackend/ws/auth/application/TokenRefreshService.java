package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.auth.domain.RefreshSession;
import com.cmsBackend.ws.auth.domain.RefreshSessionRepository;
import com.cmsBackend.ws.auth.domain.RefreshTokenHasher;
import com.cmsBackend.ws.common.security.audit.SecurityAuditService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRefreshService {
    private final RefreshSessionRepository sessions;
    private final RefreshTokenHasher refreshTokens;
    private final TokenSessionIssuer tokenSessionIssuer;
    private final AuthenticationRateLimiter rateLimiter;
    private final SecurityAuditService audit;

    public TokenRefreshService(
            RefreshSessionRepository sessions,
            RefreshTokenHasher refreshTokens,
            TokenSessionIssuer tokenSessionIssuer,
            AuthenticationRateLimiter rateLimiter,
            SecurityAuditService audit) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.tokenSessionIssuer = tokenSessionIssuer;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public AuthenticationResult refresh(String rawToken) {
        String hash = refreshTokens.hash(rawToken);
        if (!rateLimiter.allow("refresh:" + hash)) throw new AuthenticationFailureException();
        RefreshSession current = sessions.findByTokenHashForUpdate(hash)
                .orElseThrow(AuthenticationFailureException::new);
        Instant now = Instant.now();
        if (current.wasConsumed()) {
            sessions.revokeFamily(current.familyId(), now, "TOKEN_REUSE");
            audit.refreshReuseDetected(current.familyId());
            throw new RefreshTokenReuseException();
        }
        if (!current.isUsableAt(now)) {
            audit.refreshFailed();
            throw new AuthenticationFailureException();
        }
        sessions.markConsumed(current.id(), now);
        AuthenticationResult result = tokenSessionIssuer.issue(
                current.user(), current.familyId(), current.persistentSession());
        audit.refreshSucceeded(current.user().id());
        return result;
    }
}
