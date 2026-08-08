package com.cmsBackend.ws.auth.infrastructure.persistence;

import com.cmsBackend.ws.auth.domain.RefreshSession;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions", indexes = {
    @Index(name = "idx_refresh_session_family", columnList = "family_id"),
    @Index(name = "idx_refresh_session_user", columnList = "user_id")
})
public class RefreshSessionJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountJpaEntity user;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;
    private Instant revokedAt;

    private Boolean persistentSession;

    @Column(length = 80)
    private String revocationReason;

    @Version
    private long version;

    protected RefreshSessionJpaEntity() {}

    public RefreshSessionJpaEntity(
            UUID id,
            UserAccountJpaEntity user,
            UUID familyId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            boolean persistentSession) {
        this.id = id;
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.persistentSession = persistentSession;
    }

    public RefreshSession toDomain() {
        return new RefreshSession(
                id, user.toDomain(), familyId, tokenHash, createdAt, expiresAt, consumedAt, revokedAt,
                Boolean.TRUE.equals(persistentSession));
    }
}
