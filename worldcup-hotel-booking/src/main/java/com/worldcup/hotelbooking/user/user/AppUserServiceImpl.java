package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.booking.booking.BookingMapper;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import com.worldcup.hotelbooking.notification.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    public AppUserServiceImpl(AppUserRepository appUserRepository,
                              NotificationService notificationService, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Create User
    @Override
    public AppUser createUser(AppUserRequestDto dto) {
        AppUser user = AppUserMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));

        // Assign default role if none provided
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.addRole(Role.guest);
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
    public Page<AppUser> getAllUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable);
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
        AppUser existingUser = getUserById(id); // This will throw if not found
        appUserRepository.delete(existingUser);
    }

    // 7. Update User (Full update)
    @Override
    public AppUser updateUser(Long id, AppUserRequestDto dto) {
        AppUser existingUser = getUserById(id);

        existingUser.setUsername(dto.username());
        existingUser.setEmail(dto.email());
        existingUser.setPassword(passwordEncoder.encode(dto.password()));
        // Note: For security, you should hash the password here if it's plain text
        // existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        return appUserRepository.save(existingUser);
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
    public List<AppUser> searchUsers(String username, String email) {
        // Use the repository query method if you have it
        // If not, we'll create a fallback

        // Option 1: If you have the repository method
        if (username != null && email != null) {
            return appUserRepository.findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCase(username, email);
        } else if (username != null) {
            return appUserRepository.findByUsernameContainingIgnoreCase(username);
        } else if (email != null) {
            return appUserRepository.findByEmailContainingIgnoreCase(email);
        } else {
            // If both are null, return all users
            return appUserRepository.findAll();
        }
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
}