package com.cmsBackend.ws;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.domain.AuthorityCatalog;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:local-profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.security.jwt.public-key=",
    "app.security.jwt.private-key="
})
@ActiveProfiles("local")
class LocalProfileApplicationTests {
    @Autowired SpringDataUserAccountRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void contextStartsWithEphemeralLocalKeysAndFullAccessAdmin() {
        var admin = users.findByEmailIgnoreCase("admin@admin.com").orElseThrow().toDomain();
        assertThat(passwords.matches("0", admin.passwordHash())).isTrue();
        assertThat(admin.authorities()).containsExactlyInAnyOrderElementsOf(AuthorityCatalog.ALL);
    }
}
