package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingMapper;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import com.worldcup.hotelbooking.chat.ChatMessageRepository;
import com.worldcup.hotelbooking.chat.Conversation;
import com.worldcup.hotelbooking.chat.ConversationRepository;
import com.worldcup.hotelbooking.notification.NotificationRepository;
import com.worldcup.hotelbooking.notification.NotificationService;
import com.worldcup.hotelbooking.payment.PaymentRepository;
import com.worldcup.hotelbooking.review.ReviewRepository;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationRepository notificationRepository;

    public AppUserServiceImpl(
            AppUserRepository appUserRepository,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder,
            PasswordValidator passwordValidator,
            RefreshTokenRepository refreshTokenRepository,
            PaymentRepository paymentRepository,
            ReviewRepository reviewRepository,
            ChatMessageRepository chatMessageRepository,
            ConversationRepository conversationRepository, NotificationRepository notificationRepository) {
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.notificationRepository = notificationRepository;
    }

    // 1. Create User
    @Override
    public AppUser createUser(AppUserRequestDto dto) {
        passwordValidator.validate(dto.password());
        AppUser user = AppUserMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));

        // Assign default role if none provided
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.addRole(Role.GUEST);
        }


        AppUser savedUser = appUserRepository.save(user);
        appUserRepository.flush();  // 👈 force the insert to the database
        notificationService.sendWelcomeNotification(savedUser);
        return savedUser;
    }

    // 2. Get User by ID
    @Override
    @Transactional(readOnly = true)
    public AppUser getUserById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + id));
    }

    // 3. Get All Users (List version)
    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    // 4. Get All Users with Pagination
    @Override
    @Transactional(readOnly = true)
    public Page<AppUserResponseDto> getAllUsers(Pageable pageable) {
        Page<AppUser> userPage = appUserRepository.findAll(pageable);
        return userPage.map(AppUserMapper::toDto);
    }

    // 5. Get User by Email
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> getUserByEmail(String email) {
        return appUserRepository.findByEmail(email);
    }

    // 6. Delete User
    @Override
    public void deleteUser(Long id) {
        AppUser user = getUserById(id);

        // Validate active bookings
        boolean hasActiveBookings = user.getBookings().stream()
                .anyMatch(b -> b.getStatus() != Booking.BookingStatus.CANCELLED
                        && b.getStatus() != Booking.BookingStatus.CHECKED_OUT);
        if (hasActiveBookings) {
            throw new UserDeletionException("Cannot delete account with active bookings...");
        }

        List<Long> bookingIds = user.getBookings().stream()
                .map(Booking::getId)
                .collect(Collectors.toList());

        if (!bookingIds.isEmpty()) {
            paymentRepository.deleteByBookingIdIn(bookingIds);
            reviewRepository.deleteByBookingIdIn(bookingIds);
        }

        chatMessageRepository.deleteBySenderId(user.getId());

        List<Long> conversationIds = conversationRepository.findAll().stream()
                .filter(c -> c.getGuest().getId().equals(user.getId()))
                .map(Conversation::getId)
                .collect(Collectors.toList());

        if (!conversationIds.isEmpty()) {
            chatMessageRepository.deleteByConversationIds(conversationIds);
        }
        conversationRepository.deleteByGuestId(user.getId());

        refreshTokenRepository.deleteByUser(user);

        // ⭐ Delete notifications
        notificationRepository.deleteByUser(user);

        // Finally delete the user
        appUserRepository.delete(user);
    }

    // 7. Update User (Full update)
    @Override
    @Transactional
    public AppUserResponseDto updateUser(Long id, AppUserRequestDto dto) {
        AppUser existingUser = getUserById(id);

        existingUser.setUsername(dto.username());
        existingUser.setEmail(dto.email());
        existingUser.setPassword(passwordEncoder.encode(dto.password()));

        AppUser saved = appUserRepository.save(existingUser);
        return AppUserMapper.toDto(saved); // mapping inside transaction
    }

    // 8. Save User (for partial updates)
    @Override
    public AppUser saveUser(AppUser user) {
        return appUserRepository.save(user);
    }

    // 9. Get User's Bookings
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(Long userId) {
        AppUser user = getUserById(userId); // throws if not found
        return user.getBookings()
                .stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }

    // 10. Search Users (Optional - not in interface, but useful)
    @Override
    @Transactional(readOnly = true)
    public List<AppUserResponseDto> searchUsers(String username, String email) {
        List<AppUser> users;
        if (username != null && email != null) {
            users = appUserRepository.findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(username, email);
        } else if (username != null) {
            users = appUserRepository.findByUsernameContainingIgnoreCase(username);
        } else if (email != null) {
            users = appUserRepository.findByEmailContainingIgnoreCase(email);
        } else {
            users = appUserRepository.findAll();
        }
        return users.stream()
                .map(AppUserMapper::toDto)
                .collect(Collectors.toList());
    }

    // 11. Partial Update Helper
    public AppUser partialUpdateUser(Long id, java.util.Map<String, Object> updates) {
        AppUser existingUser = getUserById(id);

        updates.forEach((key, value) -> {
            switch (key.toLowerCase()) {
                case "username":
                    existingUser.setUsername((String) value);
                    break;
                case "email":
                    existingUser.setEmail((String) value);
                    break;
                case "password":
                    existingUser.setPassword(passwordEncoder.encode((String) value));
                    break;
                case "enabled":
                    if (value instanceof Boolean) {
                        existingUser.setEnabled((Boolean) value);
                    } else {
                        throw new IllegalArgumentException("Field 'enabled' must be a boolean");
                    }
                    break;
                default:
                    // Reject unknown fields
                    throw new IllegalArgumentException("Field '" + key + "' is not updatable or does not exist");
            }
        });

        return appUserRepository.save(existingUser);
    }

    @Override
    public AppUser updateUserRoles(Long id, Set<Role> roles) {
        AppUser user = getUserById(id);
        user.setRoles(roles);
        return appUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppUserResponseDto> getAllUsers(Pageable pageable, String username, String email) {
        Specification<AppUser> spec = (root, query, cb) -> cb.conjunction();

        if (username != null && !username.isBlank()) {
            spec = spec.and(UserSpecifications.usernameContains(username));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and(UserSpecifications.emailContains(email));
        }

        Page<AppUser> userPage = appUserRepository.findAll(spec, pageable);
        return userPage.map(AppUserMapper::toDto);
    }
}