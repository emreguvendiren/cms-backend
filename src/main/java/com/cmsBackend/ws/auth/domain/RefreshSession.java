package com.cmsBackend.ws.auth.domain;

import com.cmsBackend.ws.user.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        UserAccount user,
        UUID familyId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant consumedAt,
        Instant revokedAt,
        boolean persistentSession) {

    public static RefreshSession active(
            UUID id, UserAccount user, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt,
            boolean persistentSession) {
        return new RefreshSession(
                id, user, familyId, tokenHash, createdAt, expiresAt, null, null, persistentSession);
    }

    public boolean wasConsumed() {
        return consumedAt != null;
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && user.enabled();
    }
}
