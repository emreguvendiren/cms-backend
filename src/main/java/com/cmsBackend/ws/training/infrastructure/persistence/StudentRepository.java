package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.student.domain.StudentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<StudentJpaEntity, UUID> {
    Optional<StudentJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByPhoneLookupHash(String phoneLookupHash);

    @Query("""
            select s from StudentJpaEntity s
            where s.deletedAt is null
              and (:status is null or s.status = :status)
              and (:search = '' or lower(s.fullName) like lower(concat('%', :search, '%'))
                   or lower(s.email) like lower(concat('%', :search, '%'))
                   or lower(coalesce(s.activeCourse, '')) like lower(concat('%', :search, '%')))
            """)
    Page<StudentJpaEntity> search(@Param("search") String search, @Param("status") StudentStatus status,
            Pageable pageable);
}
