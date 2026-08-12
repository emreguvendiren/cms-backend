package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.ClassStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CourseClassRepository extends JpaRepository<CourseClassJpaEntity, UUID> {
    @EntityGraph(attributePaths = "course")
    @Query("""
            select cc from CourseClassJpaEntity cc
            where (:status is null or cc.status = :status)
              and (:courseId is null or cc.course.id = :courseId)
              and (:search = '' or lower(cc.name) like lower(concat('%', :search, '%'))
                   or lower(cc.code) like lower(concat('%', :search, '%'))
                   or lower(cc.instructorName) like lower(concat('%', :search, '%')))
            """)
    Page<CourseClassJpaEntity> search(
            @Param("search") String search,
            @Param("status") ClassStatus status,
            @Param("courseId") UUID courseId,
            Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCourseId(UUID courseId);

    @Override
    @EntityGraph(attributePaths = "course")
    java.util.Optional<CourseClassJpaEntity> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "course")
    @Query("select cc from CourseClassJpaEntity cc where cc.id = :id")
    java.util.Optional<CourseClassJpaEntity> findForEnrollmentById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CourseClassJpaEntity cc
            set cc.status = com.cmsBackend.ws.training.domain.ClassStatus.COMPLETED,
                cc.version = cc.version + 1
            where cc.endDate < :today
              and cc.status = com.cmsBackend.ws.training.domain.ClassStatus.IN_PROGRESS
            """)
    int completeExpiredClasses(@Param("today") java.time.LocalDate today);
}
