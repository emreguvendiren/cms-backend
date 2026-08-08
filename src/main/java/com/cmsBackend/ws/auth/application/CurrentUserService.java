package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.user.domain.UserAccount;
import com.cmsBackend.ws.user.domain.UserAccountRepository;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    @PreAuthorize("hasAuthority('profile:read')")
    @Transactional(readOnly = true)
    public UserAccount currentUser(UUID authenticatedUserId) {
        return users.findById(authenticatedUserId)
                .filter(UserAccount::enabled)
                .orElseThrow(AuthenticationFailureException::new);
    }
}
