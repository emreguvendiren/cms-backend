package com.cmsBackend.ws.auth.api.model;

import java.time.Instant;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserResponse user) {}
