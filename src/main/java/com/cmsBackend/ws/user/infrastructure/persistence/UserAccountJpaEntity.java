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

    @Column(name = "full_name", nullable = false, length = 160, columnDefinition = "varchar(160) default 'Kullanici'")
    private String fullName;

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
        this(id, email, defaultFullName(email), passwordHash, enabled, authorities);
    }

    public UserAccountJpaEntity(UUID id, String email, String fullName, String passwordHash, boolean enabled, Set<String> authorities) {
        this.id = id;
        this.email = email.toLowerCase();
        this.fullName = fullName.trim();
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = new LinkedHashSet<>(authorities);
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void replaceAuthorities(Set<String> newAuthorities) {
        authorities.clear();
        authorities.addAll(newAuthorities);
    }

    public void replacePasswordHash(String newPasswordHash) {
        passwordHash = newPasswordHash;
    }

    public void replaceFullName(String newFullName) {
        fullName = normalizeFullName(newFullName);
    }

    public UserAccount toDomain() {
        return new UserAccount(id, email, fullName, passwordHash, enabled, authorities);
    }

    private static String defaultFullName(String email) {
        String localPart = email == null ? "" : email.split("@", 2)[0];
        return normalizeFullName(localPart);
    }

    private static String normalizeFullName(String value) {
        String normalized = value == null || value.isBlank() ? "Kullanici" : value.trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
