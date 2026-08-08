package com.cmsBackend.ws.student.infrastructure.crypto;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.security.student-data")
public class StudentDataProtectionProperties {
    private int activeKeyVersion = 1;
    private String encryptionKey = "";
    private String lookupKey = "";

    @PostConstruct
    void validate() {
        require32ByteKey(encryptionKey, "STUDENT_DATA_ENCRYPTION_KEY");
        require32ByteKey(lookupKey, "STUDENT_DATA_LOOKUP_KEY");
        if (activeKeyVersion < 1) throw new IllegalStateException("Student data key version must be positive");
    }

    private void require32ByteKey(String value, String environmentName) {
        try {
            if (Base64.getDecoder().decode(value).length != 32) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(environmentName + " must be a Base64-encoded 256-bit key");
        }
    }

    public int getActiveKeyVersion() { return activeKeyVersion; }
    public void setActiveKeyVersion(int value) { this.activeKeyVersion = value; }
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String value) { this.encryptionKey = value; }
    public String getLookupKey() { return lookupKey; }
    public void setLookupKey(String value) { this.lookupKey = value; }
}
