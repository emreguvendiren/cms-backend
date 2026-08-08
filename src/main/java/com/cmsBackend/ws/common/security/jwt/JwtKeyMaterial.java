package com.cmsBackend.ws.common.security.jwt;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public record JwtKeyMaterial(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    private static final String MISSING_KEYS_MESSAGE =
            "JWT signing keys are missing. Set JWT_PUBLIC_KEY and JWT_PRIVATE_KEY, "
                    + "or activate the 'local' Spring profile for ephemeral development keys.";

    public static JwtKeyMaterial resolve(String encodedPublicKey, String encodedPrivateKey, boolean localProfile) {
        boolean publicMissing = encodedPublicKey == null || encodedPublicKey.isBlank();
        boolean privateMissing = encodedPrivateKey == null || encodedPrivateKey.isBlank();
        if (publicMissing && privateMissing && localProfile) return generateLocal();
        if (publicMissing || privateMissing) throw new IllegalStateException(MISSING_KEYS_MESSAGE);

        try {
            var factory = KeyFactory.getInstance("RSA");
            var publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey)));
            var privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedPrivateKey)));
            if (!publicKey.getModulus().equals(privateKey.getModulus())) {
                throw new IllegalStateException("JWT_PUBLIC_KEY and JWT_PRIVATE_KEY do not form the same RSA key pair.");
            }
            if (publicKey.getModulus().bitLength() < 2048) {
                throw new IllegalStateException("JWT RSA keys must be at least 2048 bits.");
            }
            return new JwtKeyMaterial(publicKey, privateKey);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "JWT signing keys are malformed. Expected Base64-encoded X.509 public and PKCS#8 private keys.",
                    exception);
        }
    }

    private static JwtKeyMaterial generateLocal() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new JwtKeyMaterial((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate local JWT signing keys.", exception);
        }
    }
}
