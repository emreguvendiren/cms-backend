package com.cmsBackend.ws.user.application;

import com.cmsBackend.ws.user.domain.AuthorityCatalog;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthorizationAdministrationService {
    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final SpringDataUserAccountRepository users;

    public AuthorizationAdministrationService(SpringDataUserAccountRepository users) { this.users = users; }

    @PreAuthorize("hasAuthority('user:permission:manage')")
    @Transactional(readOnly = true)
    public Page<ManagedUser> listUsers(String search, int page, int size) {
        return users.findByEmailContainingIgnoreCase(search.strip(), PageRequest.of(page, size, Sort.by("email").ascending()))
                .map(entity -> { var user = entity.toDomain(); return new ManagedUser(user.id(), user.email(), user.enabled(), user.authorities()); });
    }

    @PreAuthorize("hasAuthority('user:permission:manage')")
    @Transactional
    public ManagedUser replaceAuthorities(UUID actorId, UUID userId, Set<String> authorities) {
        if (!AuthorityCatalog.ALL.containsAll(authorities)) throw new UnknownAuthorityException();
        if (actorId.equals(userId) && !authorities.contains(AuthorityCatalog.USER_PERMISSION_MANAGE)) {
            throw new SelfPermissionRemovalException();
        }
        var entity = users.findById(userId).orElseThrow(UserNotFoundException::new);
        entity.replaceAuthorities(authorities);
        SECURITY_AUDIT.info("event=authorities_updated actorId={} targetUserId={} authorityCount={}", actorId, userId, authorities.size());
        var user = entity.toDomain();
        return new ManagedUser(user.id(), user.email(), user.enabled(), user.authorities());
    }

    public record ManagedUser(UUID id, String email, boolean enabled, Set<String> authorities) {}
}
