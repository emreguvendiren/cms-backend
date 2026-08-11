package com.cmsBackend.ws.user.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuthorityCatalog {
    public static final String PROFILE_READ = "profile:read";
    public static final String COURSE_READ = "course:read";
    public static final String COURSE_CREATE = "course:create";
    public static final String COURSE_UPDATE = "course:update";
    public static final String COURSE_DELETE = "course:delete";
    public static final String CLASS_READ = "class:read";
    public static final String CLASS_CREATE = "class:create";
    public static final String CLASS_UPDATE = "class:update";
    public static final String CLASS_DELETE = "class:delete";
    public static final String CLASS_ENROLLMENT_CREATE = "class:enrollment:create";
    public static final String CLASS_ENROLLMENT_UPDATE = "class:enrollment:update";
    public static final String CLASS_ENROLLMENT_DELETE = "class:enrollment:delete";
    public static final String USER_PERMISSION_MANAGE = "user:permission:manage";
    public static final String STUDENT_READ = "student:read";
    public static final String STUDENT_CREATE = "student:create";
    public static final String STUDENT_UPDATE = "student:update";
    public static final String STUDENT_DELETE = "student:delete";
    public static final String STUDENT_PHONE_REVEAL = "student:phone:reveal";
    public static final String STUDENT_IDENTITY_NUMBER_REVEAL = "student:identity-number:reveal";

    public static final List<String> ALL = List.of(
            PROFILE_READ, COURSE_READ, COURSE_CREATE, COURSE_UPDATE, COURSE_DELETE,
            CLASS_READ, CLASS_CREATE, CLASS_UPDATE, CLASS_DELETE, CLASS_ENROLLMENT_CREATE,
            CLASS_ENROLLMENT_UPDATE, CLASS_ENROLLMENT_DELETE, USER_PERMISSION_MANAGE,
            STUDENT_READ, STUDENT_CREATE, STUDENT_UPDATE, STUDENT_DELETE, STUDENT_PHONE_REVEAL,
            STUDENT_IDENTITY_NUMBER_REVEAL);

    public static final Map<String, Set<String>> ROLE_PRESETS = Map.of(
            "ADMIN", Set.copyOf(ALL),
            "TRAINING_MANAGER", Set.of(PROFILE_READ, COURSE_READ, COURSE_CREATE, COURSE_UPDATE, COURSE_DELETE,
                    CLASS_READ, CLASS_CREATE, CLASS_UPDATE, CLASS_DELETE, CLASS_ENROLLMENT_CREATE,
                    CLASS_ENROLLMENT_UPDATE, CLASS_ENROLLMENT_DELETE,
                    STUDENT_READ, STUDENT_CREATE, STUDENT_UPDATE, STUDENT_PHONE_REVEAL,
                    STUDENT_IDENTITY_NUMBER_REVEAL),
            "VIEWER", Set.of(PROFILE_READ, COURSE_READ, CLASS_READ, STUDENT_READ));

    private AuthorityCatalog() {}
}
