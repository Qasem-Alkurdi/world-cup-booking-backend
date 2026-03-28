package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.chat.ConversationRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("bookingAuthorizationService")
public class BookingAuthorizationService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final ConversationRepository conversationRepository;

    public BookingAuthorizationService(BookingRepository bookingRepository,
                                       HotelRepository hotelRepository,
                                       ConversationRepository conversationRepository) {
        this.bookingRepository      = bookingRepository;
        this.hotelRepository        = hotelRepository;
        this.conversationRepository = conversationRepository;
    }

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(userId);
    }

    public boolean isHimTheBookingUser(Long bookingId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && bookingRepository.findById(bookingId)
                .map(booking -> booking.getAppUser() != null &&
                        booking.getAppUser().getId() != null &&
                        booking.getAppUser().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBooking(Long bookingId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && bookingRepository.findById(bookingId)
                .map(booking -> booking.getHotel() != null &&
                        booking.getHotel().getOwner() != null &&
                        booking.getHotel().getOwner().getId() != null &&
                        booking.getHotel().getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBookingUsingTheHotelId(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getOwner() != null &&
                        hotel.getOwner().getId() != null &&
                        hotel.getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheBookingUser(String reference, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && bookingRepository.findByBookingReferenceWithRooms(reference)
                .map(booking -> booking.getAppUser() != null &&
                        booking.getAppUser().getId() != null &&
                        booking.getAppUser().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBooking(String reference, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && bookingRepository.findByBookingReferenceWithRooms(reference)
                .map(booking -> booking.getHotel() != null &&
                        booking.getHotel().getOwner() != null &&
                        booking.getHotel().getOwner().getId() != null &&
                        booking.getHotel().getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBookings(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getOwner() != null &&
                        hotel.getOwner().getId() != null &&
                        hotel.getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    /**
     * Used by manager chat endpoints — checks the caller is either the
     * guest or the hotel owner of a given conversation.
     */
    public boolean isConversationParticipant(Long conversationId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null &&
                conversationRepository.isParticipant(conversationId, authUserId);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");
            if (claim instanceof Integer i) return i.longValue();
            if (claim instanceof Long l)    return l;
            if (claim instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }
}