package com.cmsBackend.ws.training.infrastructure.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentJpaEntity, UUID> {
    @EntityGraph(attributePaths={"student", "payments"})
    List<ClassEnrollmentJpaEntity> findByCourseClassIdOrderByStudentFullNameAsc(UUID classId);
    boolean existsByCourseClassId(UUID classId);
    boolean existsByCourseClassIdAndStudentId(UUID classId, UUID studentId);
    Optional<ClassEnrollmentJpaEntity> findByIdAndCourseClassId(UUID id, UUID classId);
    long countByCourseClassIdAndStatusNot(UUID classId, com.cmsBackend.ws.training.domain.EnrollmentStatus status);
}
