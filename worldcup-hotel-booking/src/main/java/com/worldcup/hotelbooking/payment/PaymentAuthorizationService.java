package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.booking.booking.BookingNotFoundException;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("paymentAuthorizationService")
public class PaymentAuthorizationService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final HotelRepository hotelRepository;

    public PaymentAuthorizationService(BookingRepository bookingRepository, PaymentRepository paymentRepository, HotelRepository hotelRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.hotelRepository = hotelRepository;
    }

    public boolean canCreatPayment(Long bookingId, Authentication authentication) {
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

    public boolean canProcessPayment(String paymentIntentId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && paymentRepository.findByPaymentIntentId(paymentIntentId)
                .map(payment -> payment.getBooking() != null &&
                        payment.getBooking().getAppUser() != null &&
                        payment.getBooking().getAppUser().getId() != null &&
                        payment.getBooking().getAppUser().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean canRefundPayment(String paymentIntentId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && paymentRepository.findByPaymentIntentId(paymentIntentId)
                .map(payment -> payment.getBooking() != null &&
                        payment.getBooking().getHotel() != null &&
                        payment.getBooking().getHotel().getOwner() != null &&
                        payment.getBooking().getHotel().getOwner().getId() != null &&
                        payment.getBooking().getHotel().getOwner().getId().equals(authUserId))
                .orElse(false);
    }

    public boolean canViewPayment(Long Id, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && paymentRepository.findById(Id)
                .map(payment -> payment.getBooking() != null &&
                        ((payment.getBooking().getAppUser() != null &&
                                payment.getBooking().getAppUser().getId() != null &&
                                payment.getBooking().getAppUser().getId().equals(authUserId)) ||
                         (payment.getBooking().getHotel() != null &&
                                payment.getBooking().getHotel().getOwner() != null &&
                                payment.getBooking().getHotel().getOwner().getId() != null &&
                                payment.getBooking().getHotel().getOwner().getId().equals(authUserId))))
                .orElse(false);
    }

    public boolean canViewPaymentByBookingId(Long bookingId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && bookingRepository.findActiveBookingById(bookingId)
                .map(booking -> ((booking.getAppUser() != null &&
                        booking.getAppUser().getId() != null &&
                        booking.getAppUser().getId().equals(authUserId)) ||
                        (booking.getHotel() != null &&
                                booking.getHotel().getOwner() != null &&
                                booking.getHotel().getOwner().getId() != null &&
                                booking.getHotel().getOwner().getId().equals(authUserId))))
                .orElse(false);
    }

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && authUserId.equals(userId);
    }

    public boolean isHimTheHotelOwnerOfTheBookings(Long hotelId, Authentication authentication) {
        Long authUserId = extractUserId(authentication);
        return authUserId != null && hotelRepository.findById(hotelId)
                .map(hotel -> hotel.getOwner() != null &&
                        hotel.getOwner().getId() != null &&
                        hotel.getOwner().getId().equals(authUserId))
                .orElse(false);
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
