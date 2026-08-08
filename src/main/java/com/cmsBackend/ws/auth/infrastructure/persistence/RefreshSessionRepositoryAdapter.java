package com.cmsBackend.ws.auth.infrastructure.persistence;

import com.cmsBackend.ws.auth.domain.RefreshSession;
import com.cmsBackend.ws.auth.domain.RefreshSessionRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshSessionRepositoryAdapter implements RefreshSessionRepository {
    private final SpringDataRefreshSessionRepository repository;
    private final EntityManager entityManager;

    public RefreshSessionRepositoryAdapter(SpringDataRefreshSessionRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshSessionJpaEntity::toDomain);
    }

    @Override
    public void save(RefreshSession session) {
        UserAccountJpaEntity userReference = entityManager.getReference(UserAccountJpaEntity.class, session.user().id());
        repository.save(new RefreshSessionJpaEntity(
                session.id(),
                userReference,
                session.familyId(),
                session.tokenHash(),
                session.createdAt(),
                session.expiresAt(),
                session.persistentSession()));
    }

    @Override
    public void markConsumed(UUID sessionId, Instant consumedAt) {
        if (repository.markConsumed(sessionId, consumedAt) != 1) {
            throw new IllegalStateException("Refresh session state transition failed.");
        }
    }

    @Override
    public int revokeFamily(UUID familyId, Instant revokedAt, String reason) {
        return repository.revokeFamily(familyId, revokedAt, reason);
    }
}
