package com.cmsBackend.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cmsBackend.ws.training.infrastructure.persistence.StudentRepository;
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
    private final UUID actor = UUID.randomUUID();
    @BeforeEach void clean() { students.deleteAll(); }

    @Test void storesPhoneEncryptedAndReturnsOnlyMaskedData() throws Exception {
        String response = create("student:create").andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneMasked").value("••• ••• •• ••"))
                .andExpect(jsonPath("$.phone").doesNotExist()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1"));
        var stored = students.findById(id).orElseThrow();
        assertThat(stored.getPhoneCiphertext()).doesNotContain("5551234567");
        assertThat(stored.getPhoneIv()).isNotBlank(); assertThat(stored.getPhoneLookupHash()).hasSize(64);
        mvc.perform(get("/api/students").with(auth("student:read"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("+905551234567"))));
    }

    @Test void revealRequiresSeparateAuthorityAndDisablesCaching() throws Exception {
        String location = create("student:create").andReturn().getResponse().getHeader("Location");
        mvc.perform(post(location + "/phone/reveal").with(auth("student:read")))
                .andExpect(status().isForbidden());
        mvc.perform(post(location + "/phone/reveal").with(auth("student:phone:reveal")))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.phone").value("+905551234567"));
    }

    @Test void enforcesInactiveReasonAndDuplicatePhone() throws Exception {
        create("student:create").andExpect(status().isCreated());
        create("student:create").andExpect(status().isConflict());
        mvc.perform(post("/api/students").with(auth("student:create")).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("\"PROSPECTIVE\"", "\"INACTIVE\""))).andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions create(String authority) throws Exception {
        return mvc.perform(post("/api/students").with(auth(authority)).contentType(MediaType.APPLICATION_JSON).content(body()));
    }
    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor auth(String authority) {
        return jwt().jwt(token -> token.subject(actor.toString())).authorities(new SimpleGrantedAuthority(authority));
    }
    private String body() { return """
      {"fullName":"Deniz Arslan","email":"deniz@example.com","phone":"0555 123 45 67","status":"PROSPECTIVE",
       "registrationDate":"2026-08-06","source":"Web sitesi","kvkkConsent":true,"expectedStartDate":"2026-09-01"}
      """; }
}
