package com.cmsBackend.ws.user.application;

import com.cmsBackend.ws.user.domain.AuthorityCatalog;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthorizationAdministrationService {
    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final SpringDataUserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthorizationAdministrationService(SpringDataUserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasAuthority('user:permission:manage')")
    @Transactional(readOnly = true)
    public Page<ManagedUser> listUsers(String search, int page, int size) {
        return users.findByEmailContainingIgnoreCase(search.strip(), PageRequest.of(page, size, Sort.by("email").ascending()))
                .map(entity -> { var user = entity.toDomain(); return new ManagedUser(user.id(), user.email(), user.fullName(), user.enabled(), user.authorities()); });
    }

    @PreAuthorize("hasAuthority('user:permission:manage')")
    @Transactional
    public ManagedUser createUser(UUID actorId, String fullName, String email, String password) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) throw new DuplicateUserEmailException();
        var entity = new UserAccountJpaEntity(UUID.randomUUID(), normalizedEmail, fullName.strip(),
                passwordEncoder.encode(password), true, Set.of(AuthorityCatalog.PROFILE_READ));
        UserAccountJpaEntity saved;
        try {
            saved = users.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUserEmailException();
        }
        SECURITY_AUDIT.info("event=user_created actorId={} targetUserId={}", actorId, saved.getId());
        var user = saved.toDomain();
        return new ManagedUser(user.id(), user.email(), user.fullName(), user.enabled(), user.authorities());
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
        return new ManagedUser(user.id(), user.email(), user.fullName(), user.enabled(), user.authorities());
    }

    public record ManagedUser(UUID id, String email, String fullName, boolean enabled, Set<String> authorities) {}
}
