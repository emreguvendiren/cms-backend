package com.cmsBackend.ws.training.application;

import com.cmsBackend.ws.training.api.model.CourseResponse;
import com.cmsBackend.ws.training.api.model.CreateCourseRequest;
import com.cmsBackend.ws.training.api.model.UpdateCourseRequest;
import com.cmsBackend.ws.training.api.model.PageResponse;
import com.cmsBackend.ws.training.domain.CourseStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private final CourseRepository courses;
    private final CourseClassRepository classes;

    public CourseService(CourseRepository courses, CourseClassRepository classes) {
        this.courses = courses;
        this.classes = classes;
    }

    @PreAuthorize("hasAuthority('course:read')")
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> list(String search, CourseStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id").ascending()));
        return PageResponse.from(courses.search(normalizeSearch(search), status, pageable).map(CourseResponse::from));
    }

    @PreAuthorize("hasAuthority('course:create')")
    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        UUID id = UUID.randomUUID();
        String code = "KRS-" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        var course = new CourseJpaEntity(
                id, code, request.name().trim(), request.category().trim(), request.durationHours(),
                request.listPrice(), request.status());
        return CourseResponse.from(courses.save(course));
    }

    @PreAuthorize("hasAuthority('course:update')")
    @Transactional
    public CourseResponse update(UUID id, UpdateCourseRequest request) {
        var course = courses.findById(id).orElseThrow(TrainingNotFoundException::new);
        if (course.getVersion() != request.version()) throw new TrainingConflictException();
        course.update(request.name().trim(), request.category().trim(), request.durationHours(), request.listPrice(), request.status());
        return CourseResponse.from(courses.saveAndFlush(course));
    }

    @PreAuthorize("hasAuthority('course:delete')")
    @Transactional
    public void delete(UUID id) {
        if (!courses.existsById(id)) throw new TrainingNotFoundException();
        if (classes.existsByCourseId(id)) throw new TrainingConflictException();
        courses.deleteById(id);
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
