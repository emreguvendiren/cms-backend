package com.cmsBackend.ws.training.infrastructure.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentJpaEntity, UUID> {
    @EntityGraph(attributePaths={"student", "payments"})
    List<ClassEnrollmentJpaEntity> findByCourseClassIdOrderByStudentFullNameAsc(UUID classId);
    @EntityGraph(attributePaths={"courseClass", "courseClass.course", "payments"})
    @Query("select enrollment from ClassEnrollmentJpaEntity enrollment where enrollment.student.id = :studentId order by enrollment.courseClass.startDate desc, enrollment.courseClass.id asc")
    List<ClassEnrollmentJpaEntity> findStudentEnrollments(@Param("studentId") UUID studentId);
    boolean existsByCourseClassId(UUID classId);
    boolean existsByCourseClassIdAndStudentId(UUID classId, UUID studentId);
    Optional<ClassEnrollmentJpaEntity> findByIdAndCourseClassId(UUID id, UUID classId);
    long countByCourseClassIdAndStatusNot(UUID classId, com.cmsBackend.ws.training.domain.EnrollmentStatus status);
}
