package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.chat.ChatMessageRepository;
import com.worldcup.hotelbooking.chat.ConversationRepository;
import com.worldcup.hotelbooking.notification.NotificationRepository;
import com.worldcup.hotelbooking.notification.NotificationService;
import com.worldcup.hotelbooking.payment.PaymentRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingResponseDto;
import com.worldcup.hotelbooking.review.ReviewRepository;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

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

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private NotificationRepository notificationRepository; // NEW

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
        testUser.setBookings(new ArrayList<>());

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
        assertThat(created.getProfilePictureUrl()).isNotNull();
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
        AppUserResponseDto updated = userService.updateUser(1L, updateDto);  // ← returns DTO

        assertThat(updated.username()).isEqualTo("updated");      // record getter
        assertThat(updated.email()).isEqualTo("updated@ex.com");
    }

    @Test
    void deleteUser_existing() {
        // 1. Add a mock booking with CANCELLED status (not active)
        Booking mockBooking = new Booking();
        mockBooking.setId(100L);
        mockBooking.setStatus(Booking.BookingStatus.CANCELLED);
        testUser.setBookings(List.of(mockBooking));

        // 2. Mock repository calls
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // 3. Stub other repository methods
        doNothing().when(paymentRepository).deleteByBookingIdIn(anyList());
        doNothing().when(reviewRepository).deleteByBookingIdIn(anyList());
        doNothing().when(chatMessageRepository).deleteBySenderId(anyLong());
        doNothing().when(conversationRepository).deleteByGuestId(anyLong());
        doNothing().when(refreshTokenRepository).deleteByUser(any());
        doNothing().when(notificationRepository).deleteByUser(any()); // NEW
        when(conversationRepository.findAll()).thenReturn(new ArrayList<>());

        // 4. Act
        userService.deleteUser(1L);

        // 5. Assert
        verify(userRepository).delete(testUser);
        verify(paymentRepository, times(1)).deleteByBookingIdIn(anyList());
        verify(reviewRepository, times(1)).deleteByBookingIdIn(anyList());
        verify(chatMessageRepository, times(1)).deleteBySenderId(1L);
        verify(conversationRepository, times(1)).deleteByGuestId(1L);
        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
        verify(notificationRepository, times(1)).deleteByUser(testUser); // NEW
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
        // Prepare mock entity
        AppUser testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Role.GUEST));
        testUser.setEnabled(true);

        when(userRepository.findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase("user", "test"))
                .thenReturn(List.of(testUser));

        // Call service (returns DTOs)
        List<AppUserResponseDto> result = userService.searchUsers("user", "test");

        // Assertions
        assertThat(result).hasSize(1);
        AppUserResponseDto dto = result.get(0);
        assertThat(dto.username()).isEqualTo("user");
        assertThat(dto.email()).isEqualTo("test@example.com");
    }

    @Test
    void getUserBookings() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setAppUser(testUser);
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        booking.setHotel(hotel);

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
        verify(userRepository).save(any(AppUser.class));
        assertEquals("user", result.getUsername());
    }

    @Test
    void getUserStats_success() {
        AppUser admin = new AppUser();
        admin.setRoles(Set.of(Role.ADMIN));
        AppUser manager = new AppUser();
        manager.setRoles(Set.of(Role.MANAGER));
        AppUser guest = new AppUser();
        guest.setRoles(Set.of(Role.GUEST));

        when(userRepository.findAll()).thenReturn(List.of(admin, manager, guest));

        Map<String, Long> stats = userService.getUserStats();

        assertThat(stats.get("total")).isEqualTo(3L);
        assertThat(stats.get("admins")).isEqualTo(1L);
        assertThat(stats.get("managers")).isEqualTo(1L);
        assertThat(stats.get("regular")).isEqualTo(1L);
    }

    @Test
    void getAllUsers_withRoleFilter() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(testUser)));

        Page<AppUserResponseDto> result = userService.getAllUsers(pageable, null, null, Role.GUEST);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }
}