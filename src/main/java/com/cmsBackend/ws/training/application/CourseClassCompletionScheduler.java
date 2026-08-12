package com.cmsBackend.ws.training.application;

import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CourseClassCompletionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourseClassCompletionScheduler.class);
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Europe/Istanbul");

    private final CourseClassLifecycleService lifecycleService;

    public CourseClassCompletionScheduler(CourseClassLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Istanbul")
    public void completeExpiredClasses() {
    	LOGGER.info("LOG : Cron job has been started!");
        int completedCount = lifecycleService.completeExpiredClasses(LocalDate.now(SCHEDULE_ZONE));
        if (completedCount > 0) {
            LOGGER.info("event=course_class_auto_completion completedCount={}", completedCount);
        }
    }
}
