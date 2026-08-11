	package com.cmsBackend.ws.student.infrastructure.crypto;

public class SensitiveDataProtectionException extends RuntimeException {
    public SensitiveDataProtectionException() { super("Sensitive data operation failed"); }
}
