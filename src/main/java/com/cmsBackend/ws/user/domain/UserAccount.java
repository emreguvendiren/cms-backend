package com.cmsBackend.ws.user.domain;

import java.util.Set;
import java.util.UUID;

public record UserAccount(UUID id, String email, String fullName, String passwordHash, boolean enabled, Set<String> authorities) {
    public UserAccount {
        authorities = Set.copyOf(authorities);
    }
}
