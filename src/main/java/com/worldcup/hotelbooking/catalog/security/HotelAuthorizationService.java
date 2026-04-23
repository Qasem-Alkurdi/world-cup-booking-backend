package com.worldcup.hotelbooking.catalog.security;

import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelStatus;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("hotelAuthorizationService")
public class HotelAuthorizationService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AppUserRepository appUserRepository;

    public HotelAuthorizationService(HotelRepository hotelRepository,
                                     RoomTypeRepository roomTypeRepository,
                                     AppUserRepository appUserRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.appUserRepository = appUserRepository;
    }

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(userId);
    }

    public boolean canCreateHotelForOwner(Long ownerId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(ownerId);
    }

    public boolean canManageHotel(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        if (authUserId == null) {
            return false;
        }

        return hotelRepository.existsByIdAndOwnerIdAndStatusAndIsDeletedFalse(
                hotelId,
                authUserId,
                HotelStatus.APPROVED
        );
    }

    public boolean canViewOwnerHotels(Long ownerId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(ownerId);
    }

    public boolean canManageRoomType(Long hotelId, Long roomTypeId, Authentication authentication) {
        if (!canManageHotel(hotelId, authentication)) {
            return false;
        }

        return roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(roomTypeId, hotelId).isPresent();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");
            if (claim instanceof Integer i) {
                return i.longValue();
            }
            if (claim instanceof Long l) {
                return l;
            }
            if (claim instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }
}