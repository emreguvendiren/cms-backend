package com.cmsBackend.ws.student.application;

import com.cmsBackend.ws.common.security.audit.SecurityAuditService;
import com.cmsBackend.ws.student.api.model.*;
import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.training.api.model.PageResponse;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private final StudentRepository students;
    private final StudentPhoneProtector phones;
    private final SecurityAuditService audit;
    private final Clock clock = Clock.systemUTC();

    public StudentService(StudentRepository students, StudentPhoneProtector phones, SecurityAuditService audit) {
        this.students = students; this.phones = phones; this.audit = audit;
    }

    @PreAuthorize("hasAuthority('student:read')")
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(String search, StudentStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("fullName").ascending().and(Sort.by("id").ascending()));
        return PageResponse.from(students.search(search == null ? "" : search.trim(), status, pageable)
                .map(StudentResponse::from));
    }

    @PreAuthorize("hasAuthority('student:read')")
    @Transactional(readOnly = true)
    public StudentResponse detail(UUID id) { return StudentResponse.from(find(id)); }

    @PreAuthorize("hasAuthority('student:create')")
    @Transactional
    public StudentResponse create(CreateStudentRequest request, UUID actorId) {
        validate(request.status(), request.inactiveReason(), request.expectedStartDate());
        UUID id = UUID.randomUUID();
        ProtectedPhone protectedPhone = phones.protect(id, request.phone());
        if (students.existsByPhoneLookupHash(protectedPhone.lookupHash())) throw new StudentConflictException();
        var student = new StudentJpaEntity(id, trim(request.fullName()), request.email().trim().toLowerCase(),
                protectedPhone.ciphertext(), protectedPhone.iv(), protectedPhone.lookupHash(), protectedPhone.keyVersion(),
                request.status(), nullableTrim(request.activeCourse()), request.registrationDate(), trim(request.source()),
                request.kvkkConsent(), nullableTrim(request.inactiveReason()), request.expectedStartDate());
        try {
            StudentResponse response = StudentResponse.from(students.saveAndFlush(student));
            audit.studentChanged("create", actorId, id); return response;
        } catch (DataIntegrityViolationException exception) { throw new StudentConflictException(); }
    }

    @PreAuthorize("hasAuthority('student:update')")
    @Transactional
    public StudentResponse update(UUID id, UpdateStudentRequest request, UUID actorId) {
        validate(request.status(), request.inactiveReason(), request.expectedStartDate());
        var student = find(id);
        if (student.getVersion() != request.version()) throw new StudentConflictException();
        student.updateProfile(trim(request.fullName()), request.email().trim().toLowerCase(), request.status(),
                nullableTrim(request.activeCourse()), request.registrationDate(), trim(request.source()), request.kvkkConsent(),
                nullableTrim(request.inactiveReason()), request.expectedStartDate());
        if (request.phone() != null && !request.phone().isBlank()) {
            ProtectedPhone protectedPhone = phones.protect(id, request.phone());
            if (!protectedPhone.lookupHash().equals(student.getPhoneLookupHash())
                    && students.existsByPhoneLookupHash(protectedPhone.lookupHash())) throw new StudentConflictException();
            student.updateProtectedPhone(protectedPhone.ciphertext(), protectedPhone.iv(), protectedPhone.lookupHash(),
                    protectedPhone.keyVersion());
        }
        try {
            StudentResponse response = StudentResponse.from(students.saveAndFlush(student));
            audit.studentChanged("update", actorId, id); return response;
        } catch (DataIntegrityViolationException exception) { throw new StudentConflictException(); }
    }

    @PreAuthorize("hasAuthority('student:delete')")
    @Transactional
    public void delete(UUID id, UUID actorId) {
        var student = find(id); student.softDelete(actorId, Instant.now(clock)); students.save(student);
        audit.studentChanged("delete", actorId, id);
    }

    @PreAuthorize("hasAuthority('student:phone:reveal')")
    @Transactional(readOnly = true)
    public PhoneRevealResponse revealPhone(UUID id, UUID actorId) {
        var student = find(id);
        if (student.getPhoneCiphertext() == null || student.getPhoneIv() == null || student.getPhoneKeyVersion() == null)
            throw new StudentNotFoundException();
        String phone = phones.reveal(id, new ProtectedPhone(student.getPhoneCiphertext(), student.getPhoneIv(),
                student.getPhoneLookupHash(), student.getPhoneKeyVersion()));
        audit.studentPhoneRevealed(actorId, id);
        return new PhoneRevealResponse(phone);
    }

    private StudentJpaEntity find(UUID id) { return students.findByIdAndDeletedAtIsNull(id).orElseThrow(StudentNotFoundException::new); }
    private void validate(StudentStatus status, String inactiveReason, java.time.LocalDate expectedStartDate) {
        if (status == StudentStatus.INACTIVE && (inactiveReason == null || inactiveReason.isBlank()))
            throw new StudentValidationException("Pasif öğrenci için pasiflik nedeni zorunludur.");
        if (status != StudentStatus.INACTIVE && inactiveReason != null && !inactiveReason.isBlank())
            throw new StudentValidationException("Pasiflik nedeni yalnızca pasif öğrenciler için girilebilir.");
        if (status != StudentStatus.PROSPECTIVE && expectedStartDate != null)
            throw new StudentValidationException("Tahmini başlangıç tarihi yalnızca aday öğrenciler için girilebilir.");
    }
    private String trim(String value) { return value.trim(); }
    private String nullableTrim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
