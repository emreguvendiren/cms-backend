package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.ClassStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Formula;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_classes", indexes = {
    @Index(name = "idx_course_classes_course", columnList = "course_id"),
    @Index(name = "idx_course_classes_status_name", columnList = "status,name")
})
public class CourseClassJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJpaEntity course;

    @Column(nullable = false, length = 120)
    private String instructorName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "enrolled_count", nullable = false)
    private int storedEnrolledCount;

    @Formula("(select count(*) from class_enrollments ce where ce.class_id = id and ce.status <> 'CANCELLED')")
    private int enrolledCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ClassStatus status;

    @Version
    private long version;

    protected CourseClassJpaEntity() {}

    public CourseClassJpaEntity(
            UUID id, String code, String name, CourseJpaEntity course, String instructorName,
            LocalDate startDate, LocalDate endDate, int capacity, ClassStatus status) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("End date cannot be before start date.");
        this.id = id;
        this.code = code;
        this.name = name;
        this.course = course;
        this.instructorName = instructorName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.storedEnrolledCount = 0;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public CourseJpaEntity getCourse() { return course; }
    public String getInstructorName() { return instructorName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getCapacity() { return capacity; }
    public int getEnrolledCount() { return enrolledCount; }
    public ClassStatus getStatus() { return status; }
    public long getVersion() { return version; }

    public void update(String name, CourseJpaEntity course, String instructorName, LocalDate startDate,
                       LocalDate endDate, int capacity, ClassStatus status) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("End date cannot be before start date.");
        this.name = name;
        this.course = course;
        this.instructorName = instructorName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.status = status;
    }
}
