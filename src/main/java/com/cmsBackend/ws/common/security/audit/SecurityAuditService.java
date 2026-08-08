package com.cmsBackend.ws.common.security.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void loginSucceeded(UUID userId) {
        AUDIT.info("event=login outcome=success userId={}", userId);
    }

    public void loginFailed() {
        AUDIT.warn("event=login outcome=failure");
    }

    public void loginRateLimited() {
        AUDIT.warn("event=login outcome=rate_limited");
    }

    public void refreshSucceeded(UUID userId) {
        AUDIT.info("event=refresh outcome=success userId={}", userId);
    }

    public void refreshFailed() {
        AUDIT.warn("event=refresh outcome=failure");
    }

    public void refreshReuseDetected(UUID familyId) {
        AUDIT.error("event=refresh_reuse outcome=family_revoked familyId={}", familyId);
    }

    public void logoutSucceeded(UUID userId) {
        AUDIT.info("event=logout outcome=success userId={}", userId);
    }

    public void studentPhoneRevealed(UUID actorId, UUID studentId) {
        AUDIT.info("event=student_phone_reveal outcome=success actorId={} studentId={}", actorId, studentId);
    }

    public void studentChanged(String action, UUID actorId, UUID studentId) {
        AUDIT.info("event=student_{} outcome=success actorId={} studentId={}", action, actorId, studentId);
    }

    public void classEnrollmentCreated(UUID actorId, UUID classId, UUID studentId) {
        AUDIT.info("event=class_enrollment_create outcome=success actorId={} classId={} studentId={}",
                actorId, classId, studentId);
    }

    public void classEnrollmentChanged(String action, UUID actorId, UUID classId, UUID studentId) {
        AUDIT.info("event=class_enrollment_{} outcome=success actorId={} classId={} studentId={}",
                action, actorId, classId, studentId);
    }

}
