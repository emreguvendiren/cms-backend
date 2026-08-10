package com.cmsBackend.ws.student.application;

import java.util.UUID;

public interface StudentSensitiveDataProtector {
    ProtectedStudentSensitiveData protectPhone(UUID studentId, String phone);
    ProtectedStudentSensitiveData protectIdentityNumber(UUID studentId, String identityNumber);
    String revealPhone(UUID studentId, ProtectedStudentSensitiveData protectedPhone);
}
