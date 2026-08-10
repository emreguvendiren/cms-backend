package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.student.domain.Gender;
import com.cmsBackend.ws.student.domain.StudentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_full_name", columnList = "full_name"),
        @Index(name = "idx_students_phone_lookup", columnList = "phone_lookup_hash", unique = true),
        @Index(name = "idx_students_identity_lookup", columnList = "identity_number_lookup_hash", unique = true),
        @Index(name = "idx_students_status_deleted_name", columnList = "status,deleted_at,full_name,id")
})
public class StudentJpaEntity {
    @Id private UUID id;
    @Column(name = "full_name", nullable = false, length = 160) private String fullName;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "phone_ciphertext", length = 512) private String phoneCiphertext;
    @Column(name = "phone_iv", length = 64) private String phoneIv;
    @Column(name = "phone_lookup_hash", length = 64) private String phoneLookupHash;
    @Column(name = "phone_key_version") private Integer phoneKeyVersion;
    @Column(name = "identity_number_ciphertext", length = 512) private String identityNumberCiphertext;
    @Column(name = "identity_number_iv", length = 64) private String identityNumberIv;
    @Column(name = "identity_number_lookup_hash", length = 64) private String identityNumberLookupHash;
    @Column(name = "identity_number_key_version") private Integer identityNumberKeyVersion;
    @Enumerated(EnumType.STRING) @Column(length = 20) private StudentStatus status;
    @Column(name = "active_course", length = 160) private String activeCourse;
    @Column(name = "registration_date") private LocalDate registrationDate;
    @Column(length = 100) private String source;
    @Column(name = "kvkk_consent") private Boolean kvkkConsent;
    @Column(name = "inactive_reason", length = 500) private String inactiveReason;
    @Column(name = "expected_start_date") private LocalDate expectedStartDate;
    @Column(name = "birth_place", length = 100) private String birthPlace;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(name = "father_name", length = 120) private String fatherName;
    @Column(name = "mother_name", length = 120) private String motherName;
    @Enumerated(EnumType.STRING) @Column(length = 20) private Gender gender;
    @Column(name = "education_level", length = 100) private String educationLevel;
    @Column(name = "school_name", length = 160) private String schoolName;
    @Column(length = 120) private String profession;
    @Column(length = 500) private String address;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "deleted_by") private UUID deletedBy;
    @Version private long version;

    protected StudentJpaEntity() {}

    /** Compatibility constructor for enrollment fixtures; no phone plaintext is retained. */
    public StudentJpaEntity(UUID id, String fullName, String email, String ignoredPhone) {
        this.id = id; this.fullName = fullName; this.email = email; this.status = StudentStatus.ACTIVE;
        this.registrationDate = LocalDate.now(); this.source = "Legacy enrollment"; this.kvkkConsent = false;
    }

    public StudentJpaEntity(UUID id, String fullName, String email, String phoneCiphertext, String phoneIv,
            String phoneLookupHash, int phoneKeyVersion, String identityNumberCiphertext, String identityNumberIv,
            String identityNumberLookupHash, int identityNumberKeyVersion, StudentStatus status, String activeCourse,
            LocalDate registrationDate, String source, boolean kvkkConsent, String inactiveReason,
            LocalDate expectedStartDate, String birthPlace, LocalDate birthDate, String fatherName, String motherName,
            Gender gender, String educationLevel, String schoolName, String profession, String address) {
        this.id = id;
        updateProfile(fullName, email, status, activeCourse, registrationDate, source, kvkkConsent,
                inactiveReason, expectedStartDate, birthPlace, birthDate, fatherName, motherName, gender,
                educationLevel, schoolName, profession, address);
        updateProtectedPhone(phoneCiphertext, phoneIv, phoneLookupHash, phoneKeyVersion);
        updateProtectedIdentityNumber(identityNumberCiphertext, identityNumberIv, identityNumberLookupHash,
                identityNumberKeyVersion);
    }

    public void updateProfile(String fullName, String email, StudentStatus status, String activeCourse,
            LocalDate registrationDate, String source, boolean kvkkConsent, String inactiveReason,
            LocalDate expectedStartDate, String birthPlace, LocalDate birthDate, String fatherName, String motherName,
            Gender gender, String educationLevel, String schoolName, String profession, String address) {
        this.fullName = fullName; this.email = email; this.status = status; this.activeCourse = activeCourse;
        this.registrationDate = registrationDate; this.source = source; this.kvkkConsent = kvkkConsent;
        this.inactiveReason = inactiveReason; this.expectedStartDate = expectedStartDate;
        this.birthPlace = birthPlace; this.birthDate = birthDate; this.fatherName = fatherName; this.motherName = motherName;
        this.gender = gender; this.educationLevel = educationLevel; this.schoolName = schoolName;
        this.profession = profession; this.address = address;
    }

    public void updateProfile(String fullName, String email, StudentStatus status, String activeCourse,
            LocalDate registrationDate, String source, boolean kvkkConsent, String inactiveReason,
            LocalDate expectedStartDate) {
        updateProfile(fullName, email, status, activeCourse, registrationDate, source, kvkkConsent, inactiveReason,
                expectedStartDate, null, null, null, null, Gender.NOT_SPECIFIED, null, null, null, null);
    }

    public void updateProtectedPhone(String ciphertext, String iv, String lookupHash, int keyVersion) {
        this.phoneCiphertext = ciphertext; this.phoneIv = iv; this.phoneLookupHash = lookupHash;
        this.phoneKeyVersion = keyVersion;
    }

    public void updateProtectedIdentityNumber(String ciphertext, String iv, String lookupHash, int keyVersion) {
        this.identityNumberCiphertext = ciphertext; this.identityNumberIv = iv;
        this.identityNumberLookupHash = lookupHash; this.identityNumberKeyVersion = keyVersion;
    }

    public void softDelete(UUID actorId, Instant now) { this.deletedBy = actorId; this.deletedAt = now; }
    public void activateForEnrollment(String courseName) {
        this.status = StudentStatus.ACTIVE;
        this.activeCourse = courseName;
        this.inactiveReason = null;
        this.expectedStartDate = null;
    }
    public UUID getId(){return id;} public String getFullName(){return fullName;} public String getEmail(){return email;}
    public String getPhoneCiphertext(){return phoneCiphertext;} public String getPhoneIv(){return phoneIv;}
    public String getPhoneLookupHash(){return phoneLookupHash;} public Integer getPhoneKeyVersion(){return phoneKeyVersion;}
    public String getIdentityNumberCiphertext(){return identityNumberCiphertext;}
    public String getIdentityNumberIv(){return identityNumberIv;}
    public String getIdentityNumberLookupHash(){return identityNumberLookupHash;}
    public Integer getIdentityNumberKeyVersion(){return identityNumberKeyVersion;}
    public StudentStatus getStatus(){return status;} public String getActiveCourse(){return activeCourse;}
    public LocalDate getRegistrationDate(){return registrationDate;} public String getSource(){return source;}
    public boolean isKvkkConsent(){return Boolean.TRUE.equals(kvkkConsent);} public String getInactiveReason(){return inactiveReason;}
    public LocalDate getExpectedStartDate(){return expectedStartDate;} public Instant getDeletedAt(){return deletedAt;}
    public String getBirthPlace(){return birthPlace;} public LocalDate getBirthDate(){return birthDate;}
    public String getFatherName(){return fatherName;} public String getMotherName(){return motherName;}
    public Gender getGender(){return gender;} public String getEducationLevel(){return educationLevel;}
    public String getSchoolName(){return schoolName;} public String getProfession(){return profession;}
    public String getAddress(){return address;}
    public long getVersion(){return version;}
}
