package com.cmsBackend.ws.auth.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String password,
        Boolean rememberMe) {

    public boolean persistentSession() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
