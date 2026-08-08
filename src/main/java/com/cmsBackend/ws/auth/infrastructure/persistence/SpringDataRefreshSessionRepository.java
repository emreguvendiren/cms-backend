package com.cmsBackend.ws.auth.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRefreshSessionRepository extends JpaRepository<RefreshSessionJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct s from RefreshSessionJpaEntity s join fetch s.user u "
            + "left join fetch u.authorities where s.tokenHash = :hash")
    Optional<RefreshSessionJpaEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshSessionJpaEntity s set s.consumedAt = :consumedAt where s.id = :id")
    int markConsumed(@Param("id") UUID id, @Param("consumedAt") Instant consumedAt);

    @Modifying
    @Query("update RefreshSessionJpaEntity s set s.revokedAt = :now, s.revocationReason = :reason "
            + "where s.familyId = :familyId and s.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);
}
