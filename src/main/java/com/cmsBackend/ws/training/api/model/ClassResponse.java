package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.ClassStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassJpaEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ClassResponse(
        UUID id,
        String code,
        String name,
        UUID courseId,
        String courseCode,
        String courseName,
        String instructorName,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int enrolledCount,
        ClassStatus status,
        long version) {
    public static ClassResponse from(CourseClassJpaEntity courseClass) {
        var course = courseClass.getCourse();
        return new ClassResponse(
                courseClass.getId(), courseClass.getCode(), courseClass.getName(), course.getId(), course.getCode(),
                course.getName(), courseClass.getInstructorName(), courseClass.getStartDate(), courseClass.getEndDate(),
                courseClass.getStartTime(), courseClass.getEndTime(),
                courseClass.getCapacity(), courseClass.getEnrolledCount(), courseClass.getStatus(), courseClass.getVersion());
    }
}
