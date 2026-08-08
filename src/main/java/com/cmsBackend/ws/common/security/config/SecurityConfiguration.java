package com.cmsBackend.ws.common.security.config;

import com.cmsBackend.ws.common.security.jwt.AccessTokenTypeValidator;
import com.cmsBackend.ws.common.security.jwt.AudienceValidator;
import com.cmsBackend.ws.common.security.jwt.JwtKeyMaterial;
import com.cmsBackend.ws.common.security.web.RestSecurityHandlers;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, RestSecurityHandlers handlers, SecurityProperties properties) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        csrf.setCookieCustomizer(cookie -> cookie
                .sameSite(properties.refreshCookieSameSite())
                .secure(properties.refreshCookieSecure()));

        return http.cors(Customizer.withDefaults())
                .csrf(config -> config.csrfTokenRepository(csrf).ignoringRequestMatchers(
                        "/api/auth/login", "/api/courses/**", "/api/classes/**", "/api/admin/**"))
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasAuthority("user:permission:manage")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(config -> config.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtKeyMaterial jwtKeyMaterial(SecurityProperties properties, Environment environment) {
        return JwtKeyMaterial.resolve(
                properties.jwt().publicKey(),
                properties.jwt().privateKey(),
                environment.acceptsProfiles(Profiles.of("local")));
    }

    @Bean
    JwtEncoder jwtEncoder(JwtKeyMaterial keys) {
        var rsa = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey())
                .keyID("cms-signing-key")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties, JwtKeyMaterial keys) {
        var decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()),
                new AudienceValidator(properties.jwt().audience()),
                new AccessTokenTypeValidator()));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("authorities");
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

}
