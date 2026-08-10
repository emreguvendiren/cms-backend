package com.cmsBackend.ws.student.application;

public record ProtectedStudentSensitiveData(String ciphertext, String iv, String lookupHash, int keyVersion) {}
