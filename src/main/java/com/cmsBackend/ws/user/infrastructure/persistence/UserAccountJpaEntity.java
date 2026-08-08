package com.cmsBackend.ws.user.infrastructure.persistence;

import com.cmsBackend.ws.user.domain.UserAccount;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccountJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_authorities",
            joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_user_authorities_user", columnList = "user_id"))
    @Column(name = "authority", nullable = false, length = 100)
    private Set<String> authorities = new LinkedHashSet<>();

    protected UserAccountJpaEntity() {}

    public UserAccountJpaEntity(UUID id, String email, String passwordHash, boolean enabled, Set<String> authorities) {
        this.id = id;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = new LinkedHashSet<>(authorities);
    }

    public UUID getId() {
        return id;
    }

    public void replaceAuthorities(Set<String> newAuthorities) {
        authorities.clear();
        authorities.addAll(newAuthorities);
    }

    public void replacePasswordHash(String newPasswordHash) {
        passwordHash = newPasswordHash;
    }

    public UserAccount toDomain() {
        return new UserAccount(id, email, passwordHash, enabled, authorities);
    }
}
