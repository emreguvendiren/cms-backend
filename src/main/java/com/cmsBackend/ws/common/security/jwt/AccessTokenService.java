package com.cmsBackend.ws.common.security.jwt;

import com.cmsBackend.ws.common.security.config.SecurityProperties;
import com.cmsBackend.ws.user.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {
    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    public AccessTokenService(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedAccessToken issue(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(java.util.List.of(properties.jwt().audience()))
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("token_type", "access")
                .claim("authorities", user.authorities())
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("cms-signing-key").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {}
}
