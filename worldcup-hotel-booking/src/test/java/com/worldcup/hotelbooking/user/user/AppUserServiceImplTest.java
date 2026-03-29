package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import com.worldcup.hotelbooking.notification.notification.NotificationService;
import com.worldcup.hotelbooking.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceImplTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserServiceImpl userService;
    @Mock
    private PasswordValidator passwordValidator;

    @Captor
    private ArgumentCaptor<AppUser> userCaptor;

    private AppUser testUser;
    private AppUserRequestDto dto;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.GUEST));

        dto = new AppUserRequestDto("newuser", "new@example.com", "rawPass");
    }

    @Test
    void createUser_success() {
        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        AppUser created = userService.createUser(dto);

        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.getPassword()).isEqualTo("encodedPass");
        assertThat(created.getRoles()).containsExactly(Role.GUEST);
        verify(notificationService).sendWelcomeNotification(created);
    }

    @Test
    void createUser_withProvidedRoles() {
        // If dto had roles, but current DTO doesn't; we simulate by setting in entity manually? Not needed.
        // This test can be added if DTO includes roles.
    }

    @Test
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AppUser found = userService.getUserById(1L);
        assertThat(found).isEqualTo(testUser);
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void updateUser_full() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPass")).thenReturn("newEncoded");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUserRequestDto updateDto = new AppUserRequestDto("updated", "updated@ex.com", "newPass");
        AppUser updated = userService.updateUser(1L, updateDto);

        assertThat(updated.getUsername()).isEqualTo("updated");
        assertThat(updated.getEmail()).isEqualTo("updated@ex.com");
        assertThat(updated.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    void deleteUser_existing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(1L);

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void partialUpdateUser_validFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPass")).thenReturn("newEncoded");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> updates = Map.of(
                "username", "newName",
                "email", "new@mail.com",
                "password", "newPass",
                "enabled", false
        );

        AppUser updated = userService.partialUpdateUser(1L, updates);

        assertThat(updated.getUsername()).isEqualTo("newName");
        assertThat(updated.getEmail()).isEqualTo("new@mail.com");
        assertThat(updated.getPassword()).isEqualTo("newEncoded");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void partialUpdateUser_invalidField() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Map<String, Object> updates = Map.of("invalidField", "value");

        assertThatThrownBy(() -> userService.partialUpdateUser(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalidField");
    }

    @Test
    void updateUserRoles() {
        Set<Role> newRoles = Set.of(Role.ADMIN, Role.MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser updated = userService.updateUserRoles(1L, newRoles);

        assertThat(updated.getRoles()).isEqualTo(newRoles);
    }

    @Test
    void searchUsers_withBothParams() {
        List<AppUser> expected = List.of(testUser);
        when(userRepository.findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase("user", "test"))
                .thenReturn(expected);

        List<AppUser> result = userService.searchUsers("user", "test");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getUserBookings() {
        Booking booking = new Booking();
        booking.setId(100L);
        testUser.setBookings(List.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        List<BookingResponseDto> bookings = userService.getUserBookings(1L);

        assertThat(bookings).hasSize(1);
        // You may want to verify mapping, but that's tested elsewhere.
    }
    @Test
    void shouldThrowExceptionWhenPasswordValidationFails() {
        AppUserRequestDto dto = new AppUserRequestDto("user", "user@example.com", "weak");
        doThrow(new PasswordValidationException(java.util.List.of("Password too weak")))
                .when(passwordValidator).validate("weak");

        assertThrows(PasswordValidationException.class, () -> userService.createUser(dto));

        verify(passwordValidator).validate("weak");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldSaveUserWhenPasswordIsValid() {
        AppUserRequestDto dto = new AppUserRequestDto("user", "user@example.com", "StrongP@ss123");
        AppUser user = new AppUser();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        when(passwordEncoder.encode("StrongP@ss123")).thenReturn("encodedHash");
        when(userRepository.save(any(AppUser.class))).thenReturn(user);

        AppUser result = userService.createUser(dto);

        verify(passwordValidator).validate("StrongP@ss123");
        verify(passwordEncoder).encode("StrongP@ss123");
        verify(userRepository).save(any(AppUser.class));
        assertEquals("user", result.getUsername());
    }
}