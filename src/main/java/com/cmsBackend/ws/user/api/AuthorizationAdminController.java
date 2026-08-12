package com.cmsBackend.ws.user.api;

import com.cmsBackend.ws.user.application.AuthorizationAdministrationService;
import com.cmsBackend.ws.user.domain.AuthorityCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AuthorizationAdminController {
    private final AuthorizationAdministrationService service;
    public AuthorizationAdminController(AuthorizationAdministrationService service) { this.service = service; }

    @GetMapping("/authorization/catalog")
    public AuthorizationCatalogResponse catalog() {
        return new AuthorizationCatalogResponse(AuthorityCatalog.ALL, AuthorityCatalog.ROLE_PRESETS);
    }

    @GetMapping("/users")
    public ManagedUserPage users(@RequestParam(defaultValue = "") @Size(max = 100) String search,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        return ManagedUserPage.from(service.listUsers(search, page, size));
    }

    @PutMapping("/users/{userId}/authorities")
    public ResponseEntity<ManagedUserResponse> replace(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId, @Valid @RequestBody ReplaceAuthoritiesRequest request) {
        return ResponseEntity.ok(ManagedUserResponse.from(
                service.replaceAuthorities(UUID.fromString(jwt.getSubject()), userId, request.authorities())));
    }

    public record ReplaceAuthoritiesRequest(@NotNull @Size(max = 50) Set<@NotNull String> authorities) {}
    public record AuthorizationCatalogResponse(List<String> authorities, Map<String, Set<String>> roles) {}
    public record ManagedUserResponse(UUID id, String email, String fullName, boolean enabled, Set<String> authorities) {
        static ManagedUserResponse from(AuthorizationAdministrationService.ManagedUser user) {
            return new ManagedUserResponse(user.id(), user.email(), user.fullName(), user.enabled(), user.authorities());
        }
    }
    public record ManagedUserPage(List<ManagedUserResponse> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        static ManagedUserPage from(Page<AuthorizationAdministrationService.ManagedUser> page) {
            return new ManagedUserPage(page.getContent().stream().map(ManagedUserResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
        }
    }
}
