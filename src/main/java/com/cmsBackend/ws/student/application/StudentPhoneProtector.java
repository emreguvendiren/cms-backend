package com.cmsBackend.ws.student.application;

import java.util.UUID;

public interface StudentPhoneProtector {
    ProtectedPhone protect(UUID studentId, String phone);
    String reveal(UUID studentId, ProtectedPhone protectedPhone);
}
