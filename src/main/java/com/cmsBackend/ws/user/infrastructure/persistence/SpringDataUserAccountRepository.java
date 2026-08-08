package com.cmsBackend.ws.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpringDataUserAccountRepository extends JpaRepository<UserAccountJpaEntity, UUID> {
    @EntityGraph(attributePaths = "authorities")
    Optional<UserAccountJpaEntity> findByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = "authorities")
    Optional<UserAccountJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "authorities")
    Page<UserAccountJpaEntity> findByEmailContainingIgnoreCase(String search, Pageable pageable);
}
