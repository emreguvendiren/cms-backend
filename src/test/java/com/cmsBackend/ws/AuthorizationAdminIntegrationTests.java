package com.cmsBackend.ws;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationAdminIntegrationTests extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired SpringDataUserAccountRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    UUID actorId;
    UUID targetId;

    @BeforeEach void setUp() {
        actorId = UUID.randomUUID(); targetId = UUID.randomUUID();
        users.save(new UserAccountJpaEntity(actorId, actorId + "@example.com", "unused", true, Set.of("user:permission:manage")));
        users.save(new UserAccountJpaEntity(targetId, targetId + "@example.com", "unused", true, Set.of("profile:read")));
    }

    @Test void endpointsRequireAuthenticationAndManagementAuthority() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").with(jwt().authorities(new SimpleGrantedAuthority("profile:read"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(createUserBody("New User", "new@example.com", "StrongPass1!")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/users").with(jwt().authorities(new SimpleGrantedAuthority("profile:read")))
                        .contentType(MediaType.APPLICATION_JSON).content(createUserBody("New User", "new@example.com", "StrongPass1!")))
                .andExpect(status().isForbidden());
    }

    @Test void createsManagedUserWithEncodedPasswordAndDefaultProfileAuthority() throws Exception {
        var admin = jwt().jwt(builder -> builder.subject(actorId.toString())).authorities(new SimpleGrantedAuthority("user:permission:manage"));
        mvc.perform(post("/api/admin/users").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("New Staff", "New.Staff@Example.com", "StrongPass1!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.staff@example.com"))
                .andExpect(jsonPath("$.fullName").value("New Staff"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.authorities[0]").value("profile:read"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var created = users.findByEmailIgnoreCase("new.staff@example.com").orElseThrow().toDomain();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("StrongPass1!", created.passwordHash()));
        org.junit.jupiter.api.Assertions.assertNotEquals("StrongPass1!", created.passwordHash());
    }

    @Test void rejectsDuplicateEmailAndPasswordMismatch() throws Exception {
        var admin = jwt().jwt(builder -> builder.subject(actorId.toString())).authorities(new SimpleGrantedAuthority("user:permission:manage"));
        mvc.perform(post("/api/admin/users").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("Duplicate", targetId + "@example.com", "StrongPass1!")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_EMAIL_EXISTS"));
        mvc.perform(post("/api/admin/users").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"New Staff","email":"newer@example.com","password":"StrongPass1!","passwordConfirm":"Different1!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test void listsCatalogAndUpdatesTargetAuthorities() throws Exception {
        var admin = jwt().jwt(builder -> builder.subject(actorId.toString())).authorities(new SimpleGrantedAuthority("user:permission:manage"));
        mvc.perform(get("/api/admin/authorization/catalog").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.ADMIN").isArray());
        mvc.perform(get("/api/admin/users?search=" + targetId + "&page=0&size=10").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(targetId.toString()));
        mvc.perform(put("/api/admin/users/{id}/authorities", targetId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorities\":[\"profile:read\",\"course:read\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorities.length()").value(2));
    }

    @Test void preventsSelfLockoutAndUnknownAuthorities() throws Exception {
        var admin = jwt().jwt(builder -> builder.subject(actorId.toString())).authorities(new SimpleGrantedAuthority("user:permission:manage"));
        mvc.perform(put("/api/admin/users/{id}/authorities", actorId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"authorities\":[\"profile:read\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_PERMISSION_REMOVAL"));
        mvc.perform(put("/api/admin/users/{id}/authorities", targetId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"authorities\":[\"system:root\"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_AUTHORITY"));
    }

    private String createUserBody(String fullName, String email, String password) {
        return """
                {"fullName":"%s","email":"%s","password":"%s","passwordConfirm":"%s"}
                """.formatted(fullName, email, password, password);
    }
}
