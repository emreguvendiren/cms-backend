package com.cmsBackend.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cmsBackend.ws.training.domain.*;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.EnrollmentPaymentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class StudentApiIntegrationTests extends IntegrationTestSupport {
    @Autowired MockMvc mvc; @Autowired StudentRepository students;
    @Autowired CourseRepository courses; @Autowired CourseClassRepository classes;
    @Autowired ClassEnrollmentRepository enrollments;
    @Autowired SpringDataUserAccountRepository users;
    private final UUID actor = UUID.randomUUID();
    @BeforeEach void clean() { enrollments.deleteAll(); students.deleteAll(); classes.deleteAll(); courses.deleteAll(); }

    @Test void storesSensitiveFieldsEncryptedAndReturnsOnlyMaskedData() throws Exception {
        String response = create("student:create").andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneMasked").value("*** *** ** **"))
                .andExpect(jsonPath("$.identityNumberMasked").value("***********"))
                .andExpect(jsonPath("$.createdByUserId").value(actor.toString()))
                .andExpect(jsonPath("$.createdByFullName").value("Kayit Danismani"))
                .andExpect(jsonPath("$.note").value("Hafta ici aksam grubu ile ilgileniyor."))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.identityNumber").doesNotExist()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1"));
        var stored = students.findById(id).orElseThrow();
        assertThat(stored.getPhoneCiphertext()).doesNotContain("5551234567");
        assertThat(stored.getPhoneIv()).isNotBlank(); assertThat(stored.getPhoneLookupHash()).hasSize(64);
        assertThat(stored.getIdentityNumberCiphertext()).doesNotContain("10000000146");
        assertThat(stored.getIdentityNumberIv()).isNotBlank();
        assertThat(stored.getIdentityNumberLookupHash()).hasSize(64);
        assertThat(stored.getCreatedByUserId()).isEqualTo(actor);
        assertThat(stored.getNote()).isEqualTo("Hafta ici aksam grubu ile ilgileniyor.");
        mvc.perform(get("/api/students").with(auth("student:read"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("+905551234567"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("10000000146"))));
    }

    @Test void revealRequiresSeparateAuthorityAndDisablesCaching() throws Exception {
        String location = create("student:create").andReturn().getResponse().getHeader("Location");
        mvc.perform(post(location + "/phone/reveal").with(auth("student:read")))
                .andExpect(status().isForbidden());
        mvc.perform(post(location + "/phone/reveal").with(auth("student:phone:reveal")))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.phone").value("+905551234567"));
    }

    @Test void identityNumberRevealRequiresSeparateAuthorityAndDisablesCaching() throws Exception {
        String location = create("student:create").andReturn().getResponse().getHeader("Location");
        mvc.perform(post(location + "/identity-number/reveal").with(auth("student:read")))
                .andExpect(status().isForbidden());
        mvc.perform(post(location + "/identity-number/reveal").with(auth("student:identity-number:reveal")))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.identityNumber").value("10000000146"));
    }

    @Test void enforcesInactiveReasonAndDuplicateSensitiveData() throws Exception {
        create("student:create").andExpect(status().isCreated());
        create("student:create").andExpect(status().isConflict());
        mvc.perform(post("/api/students").with(auth("student:create")).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("\"PROSPECTIVE\"", "\"INACTIVE\""))).andExpect(status().isBadRequest());
    }

    @Test void acceptsElevenDigitIdentityNumberWithoutChecksumBlockingOperationalRegistration() throws Exception {
        mvc.perform(post("/api/students").with(auth("student:create")).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("10000000146", "12345678901").replace("deniz@example.com", "elif@example.com")
                        .replace("0555 123 45 67", "0555 123 45 68")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.identityNumberMasked").value("***********"));
    }

    @Test void listsStudentEnrollmentsWithPaymentSchedulesAndRequiresReadAuthority() throws Exception {
        var course = courses.save(new CourseJpaEntity(UUID.randomUUID(), "KRS-001", "SolidWorks Profesyonel",
                48, new BigDecimal("12500"), CourseStatus.ACTIVE));
        var courseClass = classes.save(new CourseClassJpaEntity(UUID.randomUUID(), "SNF-001", "Hafta içi akşam",
                course, "Murat Aydın", LocalDate.parse("2026-08-10"), LocalDate.parse("2026-09-02"),
                LocalTime.parse("09:00"), LocalTime.parse("18:00"), 14, ClassStatus.PLANNED));
        var student = students.save(new com.cmsBackend.ws.training.infrastructure.persistence.StudentJpaEntity(
                UUID.randomUUID(), "Deniz Arslan", "deniz@example.com", "ignored"));
        var enrollment = new ClassEnrollmentJpaEntity(UUID.randomUUID(), courseClass, student, EnrollmentStatus.ACTIVE,
                new BigDecimal("24000"), PaymentPlanType.INSTALLMENT, 2, LocalDate.parse("2026-08-15"),
                PaymentStatus.PENDING, null, "İki taksit");
        enrollment.replacePayments(List.of(
                new EnrollmentPaymentJpaEntity(UUID.randomUUID(), enrollment, 1, 2, new BigDecimal("12000"),
                        LocalDate.parse("2026-08-15"), PaymentStatus.PENDING, null),
                new EnrollmentPaymentJpaEntity(UUID.randomUUID(), enrollment, 2, 2, new BigDecimal("12000"),
                        LocalDate.parse("2026-09-15"), PaymentStatus.PENDING, null)));
        enrollments.saveAndFlush(enrollment);

        mvc.perform(get("/api/students/{id}/enrollments", student.getId()).with(auth("class:read")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/students/{id}/enrollments", student.getId()).with(auth("student:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("SolidWorks Profesyonel"))
                .andExpect(jsonPath("$[0].className").value("Hafta içi akşam"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("18:00:00"))
                .andExpect(jsonPath("$[0].paymentPlan").value("INSTALLMENT"))
                .andExpect(jsonPath("$[0].payments.length()").value(2))
                .andExpect(jsonPath("$[0].payments[0].amount").value(12000));
    }

    private org.springframework.test.web.servlet.ResultActions create(String authority) throws Exception {
        users.findById(actor).orElseGet(() -> users.save(new UserAccountJpaEntity(actor, actor + "@example.com",
                "Kayit Danismani", "unused", true, java.util.Set.of(authority))));
        return mvc.perform(post("/api/students").with(auth(authority)).contentType(MediaType.APPLICATION_JSON).content(body()));
    }
    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor auth(String authority) {
        return jwt().jwt(token -> token.subject(actor.toString())).authorities(new SimpleGrantedAuthority(authority));
    }
    private String body() { return """
      {"fullName":"Deniz Arslan","email":"deniz@example.com","phone":"0555 123 45 67","status":"PROSPECTIVE",
       "identityNumber":"10000000146","birthPlace":"Izmir","birthDate":"2001-05-20",
       "fatherName":"Mehmet","motherName":"Ayse","gender":"MALE","registrationDate":"2026-08-06",
       "source":"Web sitesi","kvkkConsent":true,"expectedStartDate":"2026-09-01",
       "educationLevel":"Lise","schoolName":"Teknik Lise","profession":"Teknisyen","address":"Konak, Izmir",
       "note":"Hafta ici aksam grubu ile ilgileniyor."}
      """; }
}
