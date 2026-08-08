package com.cmsBackend.ws.user.infrastructure;

import com.cmsBackend.ws.user.domain.AuthorityCatalog;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalAdminBootstrap implements ApplicationRunner {
    private final SpringDataUserAccountRepository users;
    private final PasswordEncoder passwords;
    public LocalAdminBootstrap(SpringDataUserAccountRepository users, PasswordEncoder passwords) { this.users = users; this.passwords = passwords; }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        var existing = users.findByEmailIgnoreCase("admin@admin.com");
        if (existing.isPresent()) {
            existing.get().replaceAuthorities(Set.copyOf(AuthorityCatalog.ALL));
            existing.get().replacePasswordHash(passwords.encode("0"));
            return;
        }
        users.save(new UserAccountJpaEntity(UUID.randomUUID(), "admin@admin.com", passwords.encode("0"), true, Set.copyOf(AuthorityCatalog.ALL)));
    }
}
