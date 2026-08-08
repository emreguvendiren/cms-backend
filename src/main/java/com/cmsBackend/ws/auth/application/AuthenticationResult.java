package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.user.domain.UserAccount;
import java.time.Instant;

public record AuthenticationResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        UserAccount user,
        String refreshToken,
        boolean persistentSession) {}
