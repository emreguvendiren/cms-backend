package com.cmsBackend.ws.auth.application;

public interface AuthenticationRateLimiter {
    boolean allow(String key);

    void clear(String key);
}
