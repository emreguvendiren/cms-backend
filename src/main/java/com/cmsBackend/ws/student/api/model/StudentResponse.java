package com.cmsBackend.ws.student.api.model;

import com.cmsBackend.ws.student.domain.Gender;
import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentJpaEntity;
import java.time.LocalDate;
import java.util.UUID;

public record StudentResponse(UUID id, String fullName, String email, boolean phoneAvailable, String phoneMasked,
        boolean identityNumberAvailable, String identityNumberMasked, String birthPlace, LocalDate birthDate,
        String fatherName, String motherName, Gender gender, StudentStatus status, String activeCourse,
        LocalDate registrationDate, String source, boolean kvkkConsent, String inactiveReason,
        LocalDate expectedStartDate, String educationLevel, String schoolName, String profession, String address,
        String note, UUID createdByUserId, String createdByFullName, long version) {
    public static StudentResponse from(StudentJpaEntity student) {
        return from(student, null);
    }

    public static StudentResponse from(StudentJpaEntity student, String createdByFullName) {
        return new StudentResponse(student.getId(), student.getFullName(), student.getEmail(),
                student.getPhoneCiphertext() != null, "*** *** ** **",
                student.getIdentityNumberCiphertext() != null, "***********", student.getBirthPlace(),
                student.getBirthDate(), student.getFatherName(), student.getMotherName(), student.getGender(),
                student.getStatus(), student.getActiveCourse(), student.getRegistrationDate(), student.getSource(),
                student.isKvkkConsent(), student.getInactiveReason(), student.getExpectedStartDate(),
                student.getEducationLevel(), student.getSchoolName(), student.getProfession(), student.getAddress(),
                student.getNote(), student.getCreatedByUserId(), createdByFullName, student.getVersion());
    }
}
