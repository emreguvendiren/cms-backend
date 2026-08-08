package com.cmsBackend.ws.student.application;

public record ProtectedPhone(String ciphertext, String iv, String lookupHash, int keyVersion) {}
