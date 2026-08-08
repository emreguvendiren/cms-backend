package com.cmsBackend.ws.user.infrastructure.security;

import com.cmsBackend.ws.user.domain.UserAccount;
import com.cmsBackend.ws.user.domain.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public UserAccountDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserAccount account = users.findByEmail(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(account.email())
                .password(account.passwordHash())
                .disabled(!account.enabled())
                .authorities(account.authorities().toArray(String[]::new))
                .build();
    }
}
