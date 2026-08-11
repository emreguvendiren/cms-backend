package com.cmsBackend.ws;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cmsBackend.ws.training.domain.*;
import com.cmsBackend.ws.training.infrastructure.persistence.*;
import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.training.application.CourseClassService;
import com.cmsBackend.ws.training.application.TrainingConflictException;
import com.cmsBackend.ws.training.api.model.CreateClassEnrollmentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TrainingApiIntegrationTests extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired CourseClassRepository classes;
    @Autowired StudentRepository students;
    @Autowired ClassEnrollmentRepository enrollments;
    @Autowired CourseClassService classService;

    @BeforeEach void setUp() { enrollments.deleteAll(); students.deleteAll(); classes.deleteAll(); courses.deleteAll(); }

    @Test void endpointsRequireFineGrainedAuthorities() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses").with(jwt().authorities(new SimpleGrantedAuthority("class:read")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses").with(jwt().authorities(new SimpleGrantedAuthority("course:read")))).andExpect(status().isOk());
        var course=activeCourse();
        mvc.perform(delete("/api/courses/{id}", course.getId()).with(jwt().authorities(new SimpleGrantedAuthority("course:update")))).andExpect(status().isForbidden());
    }

    @Test void createsCourseWithServerGeneratedCodeAndSelectedStatus() throws Exception {
        mvc.perform(post("/api/courses").with(jwt().authorities(new SimpleGrantedAuthority("course:create")))
                        .contentType(MediaType.APPLICATION_JSON).content(courseBody("ACTIVE")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code", org.hamcrest.Matchers.startsWith("KRS-")))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test void updatesCourseAndRejectsStaleVersion() throws Exception {
        var course=activeCourse(); var auth=jwt().authorities(new SimpleGrantedAuthority("course:update"));
        String body="{\"name\":\"AutoCAD Güncel\",\"durationHours\":56,\"listPrice\":14000,\"status\":\"DRAFT\",\"version\":0}";
        mvc.perform(put("/api/courses/{id}", course.getId()).with(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("AutoCAD Güncel"));
        mvc.perform(put("/api/courses/{id}", course.getId()).with(auth).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }

    @Test void protectsCourseWithClassesAndDeletesUnreferencedCourse() throws Exception {
        var referenced=activeCourse(); newClass(referenced);
        var auth=jwt().authorities(new SimpleGrantedAuthority("course:delete"));
        mvc.perform(delete("/api/courses/{id}", referenced.getId()).with(auth)).andExpect(status().isConflict());
        var removable=courses.save(new CourseJpaEntity(UUID.randomUUID(), "KRS-002", "CNC", 20, BigDecimal.TEN, CourseStatus.DRAFT));
        mvc.perform(delete("/api/courses/{id}", removable.getId()).with(auth)).andExpect(status().isNoContent());
    }

    @Test void createsAndUpdatesClassWithoutClientCode() throws Exception {
        var course=activeCourse();
        mvc.perform(post("/api/classes").with(jwt().authorities(new SimpleGrantedAuthority("class:create"))).contentType(MediaType.APPLICATION_JSON).content(classBody(course.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code", org.hamcrest.Matchers.startsWith("SNF-")));
        var item=newClass(course);
        String update="{\"name\":\"AutoCAD Güncel Sınıf\",\"courseId\":\"%s\",\"instructorName\":\"Murat Aydın\",\"startDate\":\"2026-08-11\",\"endDate\":\"2026-09-03\",\"capacity\":16,\"status\":\"IN_PROGRESS\",\"version\":0}".formatted(course.getId());
        mvc.perform(put("/api/classes/{id}", item.getId()).with(jwt().authorities(new SimpleGrantedAuthority("class:update"))).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test void returnsClassDetailStudentsAndProtectsEnrolledClassFromDeletion() throws Exception {
        var item=newClass(activeCourse());
        var student=students.save(new StudentJpaEntity(UUID.randomUUID(), "Deniz Arslan", "deniz@example.com", "05550000000"));
        enrollments.save(new ClassEnrollmentJpaEntity(UUID.randomUUID(), item, student, EnrollmentStatus.ACTIVE));
        mvc.perform(get("/api/classes/{id}", item.getId()).with(jwt().authorities(new SimpleGrantedAuthority("class:read"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.students[0].fullName").value("Deniz Arslan"))
                .andExpect(jsonPath("$.classInfo.enrolledCount").value(1));
        mvc.perform(delete("/api/classes/{id}", item.getId()).with(jwt().authorities(new SimpleGrantedAuthority("class:delete")))).andExpect(status().isConflict());
    }

    @Test void enrollsProspectiveStudentWithCashPaymentAndActivatesStudent() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("elif@example.com");
        UUID actorId = UUID.randomUUID();
        String body = """
                {"studentId":"%s","registrationFee":18500.50,"paymentPlan":"CASH",
                 "paymentStatus":"COMPLETED","note":"Peşin ödeme alındı."}
                """.formatted(student.getId());

        mvc.perform(post("/api/classes/{id}/enrollments", item.getId())
                        .with(jwt().jwt(token -> token.subject(actorId.toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:create")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Elif Yılmaz"))
                .andExpect(jsonPath("$.registrationFee").value(18500.50))
                .andExpect(jsonPath("$.paymentPlan").value("CASH"))
                .andExpect(jsonPath("$.installmentCount").doesNotExist())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].amount").value(18500.50))
                .andExpect(jsonPath("$.payments[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.payments[0].paidAt").isNotEmpty());

        var updatedStudent = students.findById(student.getId()).orElseThrow();
        Assertions.assertEquals(StudentStatus.ACTIVE, updatedStudent.getStatus());
        Assertions.assertEquals("AutoCAD 2D Teknik Çizim", updatedStudent.getActiveCourse());
        Assertions.assertEquals(1, enrollments.countByCourseClassIdAndStatusNot(item.getId(), EnrollmentStatus.CANCELLED));

        var createdEnrollment = enrollments.findByCourseClassIdOrderByStudentFullNameAsc(item.getId()).getFirst();
        String changedPaidPlan = """
                {"registrationFee":19000,"paymentPlan":"CASH","paymentStatus":"COMPLETED","version":0}
                """;
        mvc.perform(put("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), createdEnrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON).content(changedPaidPlan))
                .andExpect(status().isConflict());
    }

    @Test void enrollsStudentWithInstallmentPlanAndReturnsItInClassDetail() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("taksit@example.com");
        String body = """
                {"studentId":"%s","registrationFee":24000,"paymentPlan":"INSTALLMENT",
                 "installmentCount":6,"firstPaymentDate":"2026-08-15","paymentStatus":"PENDING",
                 "note":"Her ayın 15'inde ödeme."}
                """.formatted(student.getId());
        var authorized = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("class:enrollment:create"));

        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.installmentCount").value(6))
                .andExpect(jsonPath("$.firstPaymentDate").value("2026-08-15"))
                .andExpect(jsonPath("$.payments.length()").value(6))
                .andExpect(jsonPath("$.payments[0].dueDate").value("2026-08-15"))
                .andExpect(jsonPath("$.payments[5].dueDate").value("2027-01-15"))
                .andExpect(jsonPath("$.payments[0].amount").value(4000));

        mvc.perform(get("/api/classes/{id}", item.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students[0].paymentPlan").value("INSTALLMENT"))
                .andExpect(jsonPath("$.students[0].paymentStatus").value("PENDING"));
    }

    @Test void enrollsStudentWithPromissoryNotePlanAndReturnsItInClassDetail() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("senet@example.com");
        String body = """
                {"studentId":"%s","registrationFee":24000,"paymentPlan":"PROMISSORY_NOTE",
                 "installmentCount":4,"firstPaymentDate":"2026-08-20","paymentStatus":"PENDING",
                 "note":"Senetli kayit."}
                """.formatted(student.getId());
        var authorized = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("class:enrollment:create"));

        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentPlan").value("PROMISSORY_NOTE"))
                .andExpect(jsonPath("$.installmentCount").value(4))
                .andExpect(jsonPath("$.firstPaymentDate").value("2026-08-20"))
                .andExpect(jsonPath("$.payments.length()").value(4))
                .andExpect(jsonPath("$.payments[0].dueDate").value("2026-08-20"))
                .andExpect(jsonPath("$.payments[3].dueDate").value("2026-11-20"))
                .andExpect(jsonPath("$.payments[0].amount").value(6000));

        mvc.perform(get("/api/classes/{id}", item.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students[0].paymentPlan").value("PROMISSORY_NOTE"))
                .andExpect(jsonPath("$.students[0].payments.length()").value(4));
    }

    @Test void updatesAndDeletesClassEnrollmentWithDedicatedAuthorities() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("manage-enrollment@example.com");
        var enrollment = enrollments.saveAndFlush(new ClassEnrollmentJpaEntity(UUID.randomUUID(), item, student,
                EnrollmentStatus.ACTIVE, new BigDecimal("1000"), PaymentPlanType.CASH, null, null,
                PaymentStatus.COMPLETED, null, null));
        String updateBody = """
                {"registrationFee":2250,"paymentPlan":"CASH","paymentStatus":"PENDING",
                 "expectedPaymentDate":"2026-08-25","note":"Yeni ödeme sözü.","version":0}
                """;

        mvc.perform(put("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationFee").value(2250))
                .andExpect(jsonPath("$.expectedPaymentDate").value("2026-08-25"))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].dueDate").value("2026-08-25"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(delete("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:read"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:delete"))))
                .andExpect(status().isNoContent());
        Assertions.assertFalse(enrollments.existsById(enrollment.getId()));
    }

    @Test void receivesEnrollmentPaymentAndProtectsTheFinancialMutation() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("receive-payment@example.com");
        var enrollment = new ClassEnrollmentJpaEntity(UUID.randomUUID(), item, student, EnrollmentStatus.ACTIVE,
                new BigDecimal("1000"), PaymentPlanType.CASH, null, null, PaymentStatus.PENDING,
                LocalDate.parse("2026-08-20"), null);
        var payment = new EnrollmentPaymentJpaEntity(UUID.randomUUID(), enrollment, 1, 1,
                new BigDecimal("1000"), LocalDate.parse("2026-08-20"), PaymentStatus.PENDING, null);
        enrollment.replacePayments(List.of(payment));
        enrollment = enrollments.saveAndFlush(enrollment);
        var path = "/api/classes/{classId}/enrollments/{enrollmentId}/payments/{paymentId}/receive";

        mvc.perform(post(path, item.getId(), enrollment.getId(), payment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"paidAt\":\"2026-08-11\",\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path, item.getId(), enrollment.getId(), payment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path, item.getId(), enrollment.getId(), payment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"paidAt\":\"2026-08-11\",\"paymentMethod\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.payments[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.payments[0].paidAt").value("2026-08-11"))
                .andExpect(jsonPath("$.payments[0].paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.payments[0].version").value(1));
        mvc.perform(post(path, item.getId(), enrollment.getId(), payment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"paidAt\":\"2026-08-11\",\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isConflict());
    }

    @Test void enrollmentUpdateRejectsMissingAuthenticationWrongAuthorityAndStaleVersion() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("stale-enrollment@example.com");
        var enrollment = enrollments.saveAndFlush(new ClassEnrollmentJpaEntity(UUID.randomUUID(), item, student,
                EnrollmentStatus.ACTIVE, new BigDecimal("1000"), PaymentPlanType.CASH, null, null,
                PaymentStatus.COMPLETED, null, null));
        String body = """
                {"registrationFee":1200,"paymentPlan":"CASH","paymentStatus":"COMPLETED","version":9}
                """;
        var request = put("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:read")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/classes/{classId}/enrollments/{enrollmentId}", item.getId(), enrollment.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:enrollment:update")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test void rejectsInvalidPaymentPlanDuplicateEnrollmentAndFullClass() throws Exception {
        var course = activeCourse();
        var item = classes.save(new CourseClassJpaEntity(UUID.randomUUID(), "SNF-CAPACITY", "Tek Kişilik",
                course, "Murat Aydın", LocalDate.parse("2026-08-10"), LocalDate.parse("2026-09-02"),
                1, ClassStatus.PLANNED));
        var first = prospectiveStudent("first@example.com");
        var second = prospectiveStudent("second@example.com");
        var authorized = jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("class:enrollment:create"));

        String invalidCash = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"CASH",
                 "installmentCount":3,"firstPaymentDate":"2026-08-15","paymentStatus":"PENDING"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(invalidCash))
                .andExpect(status().isBadRequest());

        String missingExpectedDate = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"CASH","paymentStatus":"PENDING"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(missingExpectedDate))
                .andExpect(status().isBadRequest());

        String unexpectedExpectedDate = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"CASH",
                 "paymentStatus":"COMPLETED","expectedPaymentDate":"2026-08-20"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(unexpectedExpectedDate))
                .andExpect(status().isBadRequest());

        String completedInstallment = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"INSTALLMENT",
                 "installmentCount":3,"firstPaymentDate":"2026-08-15","paymentStatus":"COMPLETED"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(completedInstallment))
                .andExpect(status().isBadRequest());

        String missingPromissorySchedule = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"PROMISSORY_NOTE","paymentStatus":"PENDING"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(missingPromissorySchedule))
                .andExpect(status().isBadRequest());

        String completedPromissory = """
                {"studentId":"%s","registrationFee":1000,"paymentPlan":"PROMISSORY_NOTE",
                 "installmentCount":3,"firstPaymentDate":"2026-08-15","paymentStatus":"COMPLETED"}
                """.formatted(first.getId());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(completedPromissory))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(cashEnrollmentBody(first.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expectedPaymentDate").value("2026-08-20"));
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(cashEnrollmentBody(first.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId()).with(authorized)
                        .contentType(MediaType.APPLICATION_JSON).content(cashEnrollmentBody(second.getId())))
                .andExpect(status().isConflict());
    }

    @Test void enrollmentRequiresAuthenticationAndDedicatedAuthority() throws Exception {
        var item = newClass(activeCourse());
        var student = prospectiveStudent("denied@example.com");
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(cashEnrollmentBody(student.getId())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/classes/{id}/enrollments", item.getId())
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("class:update")))
                        .contentType(MediaType.APPLICATION_JSON).content(cashEnrollmentBody(student.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "class:update")
    void methodSecurityProtectsEnrollmentOutsideController() {
        Assertions.assertThrows(AccessDeniedException.class, () -> classService.enroll(
                UUID.randomUUID(), null, UUID.randomUUID()));
    }

    @Test void concurrentEnrollmentRequestsCannotOverbookLastSeat() throws Exception {
        var course = activeCourse();
        var item = classes.save(new CourseClassJpaEntity(UUID.randomUUID(), "SNF-CONCURRENT", "Son Kontenjan",
                course, "Murat Aydın", LocalDate.parse("2026-08-10"), LocalDate.parse("2026-09-02"),
                1, ClassStatus.PLANNED));
        var first = prospectiveStudent("concurrent-first@example.com");
        var second = prospectiveStudent("concurrent-second@example.com");
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(first, second).stream().map(student -> executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test", "", List.of(new SimpleGrantedAuthority("class:enrollment:create"))));
                try {
                    start.await();
                    classService.enroll(item.getId(), new CreateClassEnrollmentRequest(student.getId(),
                            new BigDecimal("1000"), PaymentPlanType.CASH, null, null, PaymentStatus.PENDING,
                            LocalDate.of(2026, 8, 20), null),
                            UUID.randomUUID());
                    return true;
                } catch (TrainingConflictException exception) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            long successful = 0;
            for (var future : futures) if (future.get()) successful++;
            Assertions.assertEquals(1, successful);
            Assertions.assertEquals(1, enrollments.countByCourseClassIdAndStatusNot(item.getId(), EnrollmentStatus.CANCELLED));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void validatesClassDateRangeAndReferencedCourse() throws Exception {
        var course=activeCourse(); var auth=jwt().authorities(new SimpleGrantedAuthority("class:create"));
        String invalid=classBody(course.getId()).replace("2026-08-10", "2026-10-10");
        mvc.perform(post("/api/classes").with(auth).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/classes").with(auth).contentType(MediaType.APPLICATION_JSON).content(classBody(UUID.randomUUID()))).andExpect(status().isNotFound());
    }

    private CourseJpaEntity activeCourse(){return courses.save(new CourseJpaEntity(UUID.randomUUID(), "KRS-001", "AutoCAD 2D Teknik Çizim", 48, new BigDecimal("12500"), CourseStatus.ACTIVE));}
    private CourseClassJpaEntity newClass(CourseJpaEntity course){return classes.save(new CourseClassJpaEntity(UUID.randomUUID(), "SNF-TEST", "AutoCAD Akşam", course, "Murat Aydın", LocalDate.parse("2026-08-10"), LocalDate.parse("2026-09-02"), 14, ClassStatus.PLANNED));}
    private StudentJpaEntity prospectiveStudent(String email) {
        var student = new StudentJpaEntity(UUID.randomUUID(), "Elif Yılmaz", email, "ignored");
        student.updateProfile("Elif Yılmaz", email, StudentStatus.PROSPECTIVE, null, LocalDate.parse("2026-08-01"),
                "Web sitesi", true, null, LocalDate.parse("2026-08-10"));
        return students.save(student);
    }
    private String cashEnrollmentBody(UUID studentId) {
        return "{\"studentId\":\"%s\",\"registrationFee\":1000,\"paymentPlan\":\"CASH\",\"paymentStatus\":\"PENDING\",\"expectedPaymentDate\":\"2026-08-20\"}".formatted(studentId);
    }
    private String courseBody(String status){return "{\"name\":\"AutoCAD 2D Teknik Çizim\",\"durationHours\":48,\"listPrice\":12500,\"status\":\"%s\"}".formatted(status);}
    private String classBody(UUID courseId){return "{\"name\":\"AutoCAD Hafta İçi Akşam\",\"courseId\":\"%s\",\"instructorName\":\"Murat Aydın\",\"startDate\":\"2026-08-10\",\"endDate\":\"2026-09-02\",\"capacity\":14,\"status\":\"PLANNED\"}".formatted(courseId);}
}
