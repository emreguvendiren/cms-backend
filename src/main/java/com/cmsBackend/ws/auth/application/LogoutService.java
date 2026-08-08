package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.auth.domain.RefreshSessionRepository;
import com.cmsBackend.ws.auth.domain.RefreshTokenHasher;
import com.cmsBackend.ws.common.security.audit.SecurityAuditService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService {
    private final RefreshSessionRepository sessions;
    private final RefreshTokenHasher refreshTokens;
    private final SecurityAuditService audit;

    public LogoutService(
            RefreshSessionRepository sessions, RefreshTokenHasher refreshTokens, SecurityAuditService audit) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHashForUpdate(refreshTokens.hash(rawToken)).ifPresent(session -> {
            sessions.revokeFamily(session.familyId(), Instant.now(), "LOGOUT");
            audit.logoutSucceeded(session.user().id());
        });
    }
}
