package com.cmsBackend.ws.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {
    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);

    void save(RefreshSession session);

    void markConsumed(UUID sessionId, Instant consumedAt);

    int revokeFamily(UUID familyId, Instant revokedAt, String reason);
}
