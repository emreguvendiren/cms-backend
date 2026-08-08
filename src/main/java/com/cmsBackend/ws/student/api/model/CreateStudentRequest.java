package com.cmsBackend.ws.student.api.model;

import com.cmsBackend.ws.student.domain.StudentStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank @Size(max=160) String fullName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=30) String phone,
        @NotNull StudentStatus status,
        @Size(max=160) String activeCourse,
        @NotNull LocalDate registrationDate,
        @NotBlank @Size(max=100) String source,
        boolean kvkkConsent,
        @Size(max=500) String inactiveReason,
        LocalDate expectedStartDate) {}
