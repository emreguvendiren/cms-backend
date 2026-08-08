package com.cmsBackend.ws.training.api;

import com.cmsBackend.ws.training.api.model.ClassResponse;
import com.cmsBackend.ws.training.api.model.CourseResponse;
import com.cmsBackend.ws.training.api.model.CreateClassRequest;
import com.cmsBackend.ws.training.api.model.CreateClassEnrollmentRequest;
import com.cmsBackend.ws.training.api.model.CreateCourseRequest;
import com.cmsBackend.ws.training.api.model.UpdateCourseRequest;
import com.cmsBackend.ws.training.api.model.UpdateClassRequest;
import com.cmsBackend.ws.training.api.model.UpdateClassEnrollmentRequest;
import com.cmsBackend.ws.training.api.model.ReceiveEnrollmentPaymentRequest;
import com.cmsBackend.ws.training.api.model.ClassDetailResponse;
import com.cmsBackend.ws.training.api.model.PageResponse;
import com.cmsBackend.ws.training.application.CourseClassService;
import com.cmsBackend.ws.training.application.CourseService;
import com.cmsBackend.ws.training.domain.ClassStatus;
import com.cmsBackend.ws.training.domain.CourseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TrainingController {
    private final CourseService courseService;
    private final CourseClassService classService;

    public TrainingController(CourseService courseService, CourseClassService classService) {
        this.courseService = courseService;
        this.classService = classService;
    }

    @GetMapping("/api/courses")
    public PageResponse<CourseResponse> courses(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return courseService.list(search, status, page, size);
    }

    @PostMapping("/api/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        var response = courseService.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + response.id())).body(response);
    }

    @PutMapping("/api/courses/{courseId}")
    public CourseResponse updateCourse(@PathVariable UUID courseId, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(courseId, request);
    }

    @DeleteMapping("/api/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID courseId) {
        courseService.delete(courseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/classes")
    public PageResponse<ClassResponse> classes(
            @RequestParam(defaultValue = "") @Size(max = 100) String search,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return classService.list(search, status, courseId, page, size);
    }

    @PostMapping("/api/classes")
    public ResponseEntity<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
        var response = classService.create(request);
        return ResponseEntity.created(URI.create("/api/classes/" + response.id())).body(response);
    }

    @GetMapping("/api/classes/{classId}")
    public ClassDetailResponse classDetail(@PathVariable UUID classId) { return classService.detail(classId); }

    @PostMapping("/api/classes/{classId}/enrollments")
    public ResponseEntity<ClassDetailResponse.EnrolledStudentResponse> createClassEnrollment(
            @PathVariable UUID classId, @Valid @RequestBody CreateClassEnrollmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var response = classService.enroll(classId, request, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.created(URI.create("/api/classes/" + classId + "/enrollments/" + response.id()))
                .body(response);
    }

    @PutMapping("/api/classes/{classId}/enrollments/{enrollmentId}")
    public ClassDetailResponse.EnrolledStudentResponse updateClassEnrollment(
            @PathVariable UUID classId, @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateClassEnrollmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return classService.updateEnrollment(classId, enrollmentId, request, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/api/classes/{classId}/enrollments/{enrollmentId}/payments/{paymentId}/receive")
    public ClassDetailResponse.EnrolledStudentResponse receiveEnrollmentPayment(
            @PathVariable UUID classId, @PathVariable UUID enrollmentId, @PathVariable UUID paymentId,
            @Valid @RequestBody ReceiveEnrollmentPaymentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return classService.receivePayment(classId, enrollmentId, paymentId, request,
                UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/api/classes/{classId}/enrollments/{enrollmentId}")
    public ResponseEntity<Void> deleteClassEnrollment(
            @PathVariable UUID classId, @PathVariable UUID enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        classService.deleteEnrollment(classId, enrollmentId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/classes/{classId}")
    public ClassResponse updateClass(@PathVariable UUID classId, @Valid @RequestBody UpdateClassRequest request) {
        return classService.update(classId, request);
    }

    @DeleteMapping("/api/classes/{classId}")
    public ResponseEntity<Void> deleteClass(@PathVariable UUID classId) {
        classService.delete(classId);
        return ResponseEntity.noContent().build();
    }
}
