package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.auth.domain.RefreshSession;
import com.cmsBackend.ws.auth.domain.RefreshSessionRepository;
import com.cmsBackend.ws.auth.domain.RefreshTokenGenerator;
import com.cmsBackend.ws.auth.domain.RefreshTokenHasher;
import com.cmsBackend.ws.common.security.config.SecurityProperties;
import com.cmsBackend.ws.common.security.jwt.AccessTokenService;
import com.cmsBackend.ws.user.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenSessionIssuer {
    private final RefreshSessionRepository sessions;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AccessTokenService accessTokens;
    private final SecurityProperties properties;

    public TokenSessionIssuer(
            RefreshSessionRepository sessions,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenHasher refreshTokenHasher,
            AccessTokenService accessTokens,
            SecurityProperties properties) {
        this.sessions = sessions;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenHasher = refreshTokenHasher;
        this.accessTokens = accessTokens;
        this.properties = properties;
    }

    @Transactional
    public AuthenticationResult issue(UserAccount user, UUID familyId, boolean persistentSession) {
        String rawToken = refreshTokenGenerator.generate();
        Instant now = Instant.now();
        sessions.save(RefreshSession.active(
                UUID.randomUUID(),
                user,
                familyId,
                refreshTokenHasher.hash(rawToken),
                now,
                now.plus(properties.refreshTokenTtl()),
                persistentSession));
        var access = accessTokens.issue(user);
        return new AuthenticationResult(access.value(), access.expiresAt(), user, rawToken, persistentSession);
    }
}
