package com.cmsBackend.ws.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);
}
