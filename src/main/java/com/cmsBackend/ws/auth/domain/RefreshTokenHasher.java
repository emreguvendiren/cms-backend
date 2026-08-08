package com.cmsBackend.ws.auth.domain;

public interface RefreshTokenHasher {
    String hash(String rawToken);
}
