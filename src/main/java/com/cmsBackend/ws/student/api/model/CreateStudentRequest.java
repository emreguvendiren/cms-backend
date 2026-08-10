package com.cmsBackend.ws.student.api.model;

import com.cmsBackend.ws.student.domain.Gender;
import com.cmsBackend.ws.student.domain.StudentStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank @Size(max=160) String fullName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=30) String phone,
        @NotBlank @Size(min=11, max=11) String identityNumber,
        @Size(max=100) String birthPlace,
        LocalDate birthDate,
        @Size(max=120) String fatherName,
        @Size(max=120) String motherName,
        @NotNull Gender gender,
        @NotNull StudentStatus status,
        @Size(max=160) String activeCourse,
        @NotNull LocalDate registrationDate,
        @NotBlank @Size(max=100) String source,
        boolean kvkkConsent,
        @Size(max=500) String inactiveReason,
        LocalDate expectedStartDate,
        @Size(max=100) String educationLevel,
        @Size(max=160) String schoolName,
        @Size(max=120) String profession,
        @Size(max=500) String address) {}
