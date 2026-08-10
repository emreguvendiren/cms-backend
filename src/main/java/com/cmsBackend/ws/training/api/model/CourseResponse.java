package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.CourseStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseJpaEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record CourseResponse(
        UUID id, String code, String name, int durationHours, BigDecimal listPrice,
        CourseStatus status, long version) {
    public static CourseResponse from(CourseJpaEntity course) {
        return new CourseResponse(
                course.getId(), course.getCode(), course.getName(), course.getDurationHours(),
                course.getListPrice(), course.getStatus(), course.getVersion());
    }
}
