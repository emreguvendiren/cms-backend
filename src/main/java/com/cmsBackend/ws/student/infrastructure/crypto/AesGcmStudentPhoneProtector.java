package com.cmsBackend.ws.student.infrastructure.crypto;

import com.cmsBackend.ws.student.application.ProtectedPhone;
import com.cmsBackend.ws.student.application.StudentPhoneProtector;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesGcmStudentPhoneProtector implements StudentPhoneProtector {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final StudentDataProtectionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmStudentPhoneProtector(StudentDataProtectionProperties properties) { this.properties = properties; }

    @Override
    public ProtectedPhone protect(UUID studentId, String phone) {
        String normalized = normalize(phone);
        byte[] iv = new byte[IV_BYTES]; secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(studentId));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return new ProtectedPhone(Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv), lookupHash(normalized), properties.getActiveKeyVersion());
        } catch (GeneralSecurityException exception) {
            throw new SensitiveDataProtectionException();
        }
    }

    @Override
    public String reveal(UUID studentId, ProtectedPhone value) {
        if (value.keyVersion() != properties.getActiveKeyVersion()) throw new SensitiveDataProtectionException();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_BITS,
                    Base64.getDecoder().decode(value.iv())));
            cipher.updateAAD(aad(studentId));
            return new String(cipher.doFinal(Base64.getDecoder().decode(value.ciphertext())), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new SensitiveDataProtectionException();
        }
    }

    private SecretKeySpec encryptionKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(properties.getEncryptionKey()), "AES");
    }

    private String lookupHash(String normalized) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(properties.getLookupKey()), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] aad(UUID studentId) {
        return ("student:" + studentId + ":phone:v1").getBytes(StandardCharsets.UTF_8);
    }

    private String normalize(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.length() == 10) digits = "90" + digits;
        else if (digits.length() == 11 && digits.startsWith("0")) digits = "90" + digits.substring(1);
        if (!digits.matches("90[1-9][0-9]{9}")) throw new IllegalArgumentException("Invalid Turkish phone number");
        return "+" + digits;
    }
}
