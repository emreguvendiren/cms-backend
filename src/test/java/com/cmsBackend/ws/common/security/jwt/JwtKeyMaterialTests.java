package com.cmsBackend.ws.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtKeyMaterialTests {
    @Test
    void localProfileGeneratesAnEphemeralStrongKeyPair() {
        JwtKeyMaterial keys = JwtKeyMaterial.resolve("", "", true);

        assertThat(keys.publicKey().getModulus()).isEqualTo(keys.privateKey().getModulus());
        assertThat(keys.publicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    @Test
    void nonLocalProfileFailsClosedWhenKeysAreMissing() {
        assertThatThrownBy(() -> JwtKeyMaterial.resolve("", "", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PUBLIC_KEY")
                .hasMessageContaining("local");
    }

    @Test
    void partialKeyConfigurationFailsEvenForLocalProfile() {
        assertThatThrownBy(() -> JwtKeyMaterial.resolve("public-only", "", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY");
    }
}
