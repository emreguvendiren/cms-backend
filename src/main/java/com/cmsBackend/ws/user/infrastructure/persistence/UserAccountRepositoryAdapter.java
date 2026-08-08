package com.cmsBackend.ws.user.infrastructure.persistence;

import com.cmsBackend.ws.user.domain.UserAccount;
import com.cmsBackend.ws.user.domain.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepositoryAdapter implements UserAccountRepository {
    private final SpringDataUserAccountRepository repository;

    public UserAccountRepositoryAdapter(SpringDataUserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(UserAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(UserAccountJpaEntity::toDomain);
    }
}
