package com.cmsBackend.ws.auth.api.model;

import com.cmsBackend.ws.user.domain.UserAccount;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String email, Set<String> authorities) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.id(), user.email(), user.authorities());
    }
}
