package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.CourseStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<CourseJpaEntity, UUID> {
    @Query("""
            select c from CourseJpaEntity c
            where (:status is null or c.status = :status)
              and (:search = '' or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(c.code) like lower(concat('%', :search, '%'))
                   or lower(c.category) like lower(concat('%', :search, '%')))
            """)
    Page<CourseJpaEntity> search(
            @Param("search") String search, @Param("status") CourseStatus status, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
}
