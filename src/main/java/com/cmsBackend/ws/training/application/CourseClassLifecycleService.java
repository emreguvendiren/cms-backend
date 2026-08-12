package com.cmsBackend.ws.training.application;

import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseClassLifecycleService {
    private final CourseClassRepository classes;

    public CourseClassLifecycleService(CourseClassRepository classes) {
        this.classes = classes;
    }

    @Transactional
    public int completeExpiredClasses(LocalDate today) {
        return classes.completeExpiredClasses(today);
    }
}
