package com.cmsBackend.ws.student.api.model;

import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentJpaEntity;
import java.time.LocalDate;
import java.util.UUID;

public record StudentResponse(UUID id, String fullName, String email, boolean phoneAvailable, String phoneMasked,
        StudentStatus status, String activeCourse, LocalDate registrationDate, String source, boolean kvkkConsent,
        String inactiveReason, LocalDate expectedStartDate, long version) {
    public static StudentResponse from(StudentJpaEntity student) {
        return new StudentResponse(student.getId(), student.getFullName(), student.getEmail(),
                student.getPhoneCiphertext() != null, "••• ••• •• ••", student.getStatus(), student.getActiveCourse(),
                student.getRegistrationDate(), student.getSource(), student.isKvkkConsent(), student.getInactiveReason(),
                student.getExpectedStartDate(), student.getVersion());
    }
}
