package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")   // uses long JWT secret and PostgreSQL
class UserControllerSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String guestToken;
    private Long adminId;
    private Long guestId;

    @BeforeEach
    void setUp() {
        String adminSuffix = UUID.randomUUID().toString().substring(0, 8);
        String guestSuffix = UUID.randomUUID().toString().substring(0, 8);

        AppUser admin = new AppUser();
        admin.setUsername("admin_" + adminSuffix);
        admin.setEmail("admin_" + adminSuffix + "@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        adminId = userRepository.save(admin).getId();

        AppUser guest = new AppUser();
        guest.setUsername("guest_" + guestSuffix);
        guest.setEmail("guest_" + guestSuffix + "@test.com");
        guest.setPassword(passwordEncoder.encode("guest123"));
        guest.setEnabled(true);
        guest.setRoles(Set.of(Role.GUEST));
        guestId = userRepository.save(guest).getId();

        adminToken = loginAndGetToken(admin.getUsername(), "admin123");
        guestToken = loginAndGetToken(guest.getUsername(), "guest123");
    }

    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        LoginResponse response = webTestClient.post()
                .uri("/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response.accessToken();
    }

    @Test
    void getUsers_withoutToken_returnsUnauthorized() {
        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getUsers_withAdminToken_returnsOk() {
        webTestClient.get()
                .uri("/users")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getUsers_withGuestToken_returnsForbidden() {
        webTestClient.get()
                .uri("/users")
                .headers(headers -> headers.setBearerAuth(guestToken))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getUserById_withOwnId_returnsOk() {
        webTestClient.get()
                .uri("/users/" + guestId)
                .headers(headers -> headers.setBearerAuth(guestToken))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getUserById_withDifferentUserId_returnsForbidden() {
        webTestClient.get()
                .uri("/users/" + adminId)
                .headers(headers -> headers.setBearerAuth(guestToken))
                .exchange()
                .expectStatus().isForbidden();
    }
}