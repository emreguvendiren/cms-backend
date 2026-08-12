package com.cmsBackend.ws.student.api;

import com.cmsBackend.ws.student.api.model.*;
import com.cmsBackend.ws.student.application.StudentService;
import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.training.api.model.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service = service; }

    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(defaultValue="") @Size(max=100) String search,
            @RequestParam(required=false) StudentStatus status, @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return service.list(search, status, page, size);
    }
    @GetMapping("/{id}") public StudentResponse detail(@PathVariable UUID id) { return service.detail(id); }
    @GetMapping("/{id}/enrollments") public List<StudentEnrollmentResponse> enrollments(@PathVariable UUID id) {
        return service.enrollments(id);
    }
    @PostMapping public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request, @AuthenticationPrincipal Jwt jwt) {
        var response = service.create(request, actor(jwt));
        return ResponseEntity.created(URI.create("/api/students/" + response.id())).body(response);
    }
    @PutMapping("/{id}") public StudentResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request, @AuthenticationPrincipal Jwt jwt) { return service.update(id, request, actor(jwt)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, actor(jwt)); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/phone/reveal")
    public ResponseEntity<PhoneRevealResponse> reveal(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache").body(service.revealPhone(id, actor(jwt)));
    }
    @PostMapping("/{id}/identity-number/reveal")
    public ResponseEntity<IdentityNumberRevealResponse> revealIdentityNumber(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache").body(service.revealIdentityNumber(id, actor(jwt)));
    }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
