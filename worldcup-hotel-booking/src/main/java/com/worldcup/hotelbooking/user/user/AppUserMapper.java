package com.worldcup.hotelbooking.user.user;

import java.util.Set;

public class AppUserMapper {

    private AppUserMapper() {
    }


    public static AppUser toEntity(AppUserRequestDto dto) {

        AppUser user = new AppUser();
        user.setUsername(dto.username());
        user.setEmail(dto.email());

        // Plain assignment for Step 1
        user.setPassword(dto.password());

        user.setRoles(Set.of(Role.guest));
        user.setEnabled(true);

        return user;
    }


    public static AppUserResponseDto toDto(AppUser user) {

        return new AppUserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.isEnabled(),
                user.getBookings() == null ? null :
                        user.getBookings()
                                .stream()
                                .map(com.worldcup.hotelbooking.booking.booking.BookingMapper::toDto)
                                .toList()
        );
    }
}
