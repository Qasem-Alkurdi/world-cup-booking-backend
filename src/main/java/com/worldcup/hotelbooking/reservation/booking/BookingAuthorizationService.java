package com.worldcup.hotelbooking.reservation.booking;

import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.chat.ConversationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("bookingAuthorizationService")
public class BookingAuthorizationService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final ConversationRepository conversationRepository;

    public BookingAuthorizationService(BookingRepository bookingRepository,
                                       HotelRepository hotelRepository,
                                       ConversationRepository conversationRepository) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.conversationRepository = conversationRepository;
    }

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(userId);
    }

    // ─── By booking ID ────────────────────────────────────────────────────────

    public boolean isHimTheBookingUser(Long bookingId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        if (authUserId == null) return false;

        bookingRepository.findActiveBookingById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        // Use the dedicated JPQL query that selects ONLY the active booking by id
        // without joining on the self-referencing snapshot relationship.
        // Plain findById() triggers Hibernate to JOIN booking → booking via
        // snapshot_of_booking_reference, which returns empty when there is no
        // snapshot row — causing a NoSuchElementException even though the booking exists.
        return bookingRepository.findActiveBookingById(bookingId)
                .map(booking -> booking.getAppUser() != null
                        && booking.getAppUser().getId() != null
                        && booking.getAppUser().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBooking(Long bookingId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        Optional<Long> ownerId = bookingRepository.findHotelOwnerIdByBookingId(bookingId);
        return ownerId.map(id -> id.equals(userId)).orElse(false);
    }

    // ─── By hotel ID ──────────────────────────────────────────────────────────

    public boolean isHimTheHotelOwnerOfTheBookingUsingTheHotelId(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getOwner() != null
                        && hotel.getOwner().getId() != null
                        && hotel.getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBookings(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getOwner() != null
                        && hotel.getOwner().getId() != null
                        && hotel.getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    // ─── By booking reference ─────────────────────────────────────────────────

    public boolean isHimTheBookingUser(String reference, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        if (authUserId == null) return false;
        bookingRepository.findByBookingReferenceWithRooms(reference).orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + reference));

        return bookingRepository.findByBookingReferenceWithRooms(reference)
                .map(booking -> booking.getAppUser() != null
                        && booking.getAppUser().getId() != null
                        && booking.getAppUser().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean isHimTheHotelOwnerOfTheBooking(String reference, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        if (authUserId == null) return false;
        bookingRepository.findByBookingReferenceWithRooms(reference).orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + reference));


        return bookingRepository.findByBookingReferenceWithRooms(reference)
                .map(booking -> booking.getHotel() != null
                        && booking.getHotel().getOwner() != null
                        && booking.getHotel().getOwner().getId() != null
                        && booking.getHotel().getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    public boolean isConversationParticipant(Long conversationId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null
                && conversationRepository.isParticipant(conversationId, authUserId);
    }

    // ─── JWT extraction ───────────────────────────────────────────────────────

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");
            if (claim instanceof Integer i) return i.longValue();
            if (claim instanceof Long l) return l;
            if (claim instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}