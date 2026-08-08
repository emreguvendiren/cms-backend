package com.cmsBackend.ws;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmsBackend.ws.auth.infrastructure.persistence.SpringDataRefreshSessionRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired SpringDataUserAccountRepository users;
    @Autowired SpringDataRefreshSessionRepository sessions;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtEncoder jwtEncoder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        sessions.deleteAll();
        users.deleteAll();
        userId = UUID.randomUUID();
        users.save(new UserAccountJpaEntity(
                userId, "admin@example.com", passwordEncoder.encode("Correct-Horse-42"), true, Set.of("profile:read")));
    }

    @Test
    void loginReturnsShortLivedAccessTokenAndSecureOpaqueRefreshCookie() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("Correct-Horse-42")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(cookie().httpOnly("cms_refresh", true))
                .andExpect(cookie().secure("cms_refresh", true))
                .andExpect(cookie().maxAge("cms_refresh", -1))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(content().string(not(containsString("cms_refresh"))));
    }

    @Test
    void rememberMePersistsRefreshCookieAndRotationPreservesPreference() throws Exception {
        Cookie first = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"Correct-Horse-42\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("cms_refresh", 30 * 24 * 60 * 60))
                .andReturn().getResponse().getCookie("cms_refresh");

        org.assertj.core.api.Assertions.assertThat(first).isNotNull();
        mvc.perform(post("/api/auth/refresh").cookie(first).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("cms_refresh", 30 * 24 * 60 * 60));
    }

    @Test
    void invalidAndUnknownCredentialsHaveSameGenericResponse() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void protectedEndpointRejectsMissingMalformedAndWrongAudienceTokens() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer malformed"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("wrong-audience", "access")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("cms-spa", "refresh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void decoderRejectsInvalidSignatureIssuerExpirationAndNoneAlgorithm() throws Exception {
        String valid = token("cms-spa", "access");
        String[] parts = valid.split("\\.");
        parts[2] = (parts[2].startsWith("a") ? "b" : "a") + parts[2].substring(1);
        String invalidSignature = String.join(".", parts);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignature))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWith("wrong-issuer", java.time.Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWith("cms-api", java.time.Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized());
        var base64Url = java.util.Base64.getUrlEncoder().withoutPadding();
        String none = base64Url.encodeToString("{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "." + base64Url.encodeToString(("{\"sub\":\"" + userId + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".";
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + none))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        users.deleteAll();
        users.save(new UserAccountJpaEntity(
                userId, "admin@example.com", passwordEncoder.encode("Correct-Horse-42"), false, Set.of("profile:read")));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("Correct-Horse-42")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validBearerTokenReachesMethodAndResourceAuthorization() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("cms-spa", "access")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void methodAuthorizationReturnsForbiddenWithoutRequiredAuthority() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutProfileAuthority()))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRequiresCsrfRotatesTokenAndReuseRevokesFamily() throws Exception {
        Cookie first = loginCookie();
        mvc.perform(post("/api/auth/refresh").cookie(first)).andExpect(status().isForbidden());

        Cookie second = mvc.perform(post("/api/auth/refresh").cookie(first).with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("cms_refresh");

        org.assertj.core.api.Assertions.assertThat(second).isNotNull();
        org.assertj.core.api.Assertions.assertThat(second.getValue()).isNotEqualTo(first.getValue());

        mvc.perform(post("/api/auth/refresh").cookie(first).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(second).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshRequestsCannotBothSucceed() throws Exception {
        Cookie refresh = loginCookie();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var request = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                return mvc.perform(post("/api/auth/refresh").cookie(refresh).with(csrf()))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(request);
            var second = executor.submit(request);
            start.countDown();
            Set<Integer> statuses = Set.of(first.get(), second.get());
            org.assertj.core.api.Assertions.assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void logoutRequiresCsrfAndClearsCookie() throws Exception {
        Cookie refresh = loginCookie();
        mvc.perform(post("/api/auth/logout").cookie(refresh)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(refresh).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void corsAllowsOnlyConfiguredExactOrigin() throws Exception {
        mvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        mvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private Cookie loginCookie() throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("Correct-Horse-42")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("cms_refresh");
    }

    private String token(String audience, String type) {
        var now = java.time.Instant.now();
        var claims = JwtClaimsSet.builder().issuer("cms-api").audience(java.util.List.of(audience))
                .subject(userId.toString()).issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("token_type", type).claim("authorities", Set.of("profile:read")).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("cms-signing-key").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String tokenWith(String issuer, java.time.Instant expiration) {
        var now = expiration.isAfter(java.time.Instant.now())
                ? java.time.Instant.now().minusSeconds(60)
                : expiration.minusSeconds(60);
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(java.util.List.of("cms-spa"))
                .subject(userId.toString()).issuedAt(now).expiresAt(expiration)
                .claim("token_type", "access").claim("authorities", Set.of("profile:read")).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("cms-signing-key").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String tokenWithoutProfileAuthority() {
        var now = java.time.Instant.now();
        var claims = JwtClaimsSet.builder().issuer("cms-api").audience(java.util.List.of("cms-spa"))
                .subject(userId.toString()).issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("token_type", "access").claim("authorities", Set.of("content:read")).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("cms-signing-key").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String loginBody(String password) {
        return "{\"email\":\"admin@example.com\",\"password\":\"" + password + "\"}";
    }
}
