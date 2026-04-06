package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // use real DB
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository userRepository;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CacheManager cacheManager;

    private AppUser user1, user2;

    @BeforeEach
    void setUp() {
        user1 = new AppUser();
        user1.setUsername("john_doe");
        user1.setEmail("john@example.com");
        user1.setPassword("pass");
        user1.setRoles(Set.of(Role.GUEST));
        user1.setEnabled(true);
        userRepository.save(user1);

        user2 = new AppUser();
        user2.setUsername("jane_smith");
        user2.setEmail("jane@example.com");
        user2.setPassword("pass");
        user2.setRoles(Set.of(Role.ADMIN));
        user2.setEnabled(true);
        userRepository.save(user2);
    }

    @Test
    void findByUsername_shouldReturnUser() {
        Optional<AppUser> found = userRepository.findByUsername("john_doe");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByEmail_shouldReturnUser() {
        Optional<AppUser> found = userRepository.findByEmail("jane@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jane_smith");
    }

    @Test
    void findByUsernameContainingIgnoreCase_shouldReturnMatches() {
        List<AppUser> results = userRepository.findByUsernameContainingIgnoreCase("JOHN");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("john_doe");
    }

    @Test
    void findByEmailContainingIgnoreCase_shouldReturnMatches() {
        List<AppUser> results = userRepository.findByEmailContainingIgnoreCase("JANE");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase_shouldReturnIntersection() {
        List<AppUser> results = userRepository
                .findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase("john", "example");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("john_doe");
    }
}