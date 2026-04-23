package com.worldcup.hotelbooking.reservation.cancellation;

import com.worldcup.hotelbooking.reservation.booking.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CancellationPolicyServiceImplTest {

    private final CancellationPolicyServiceImpl service = new CancellationPolicyServiceImpl();

    private Booking buildBooking(Booking.BookingStatus status, LocalDate checkInDate, BigDecimal totalPrice) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("WC-TEST-REF");
        booking.setStatus(status);
        booking.setCheckInDate(checkInDate);
        booking.setTotalPrice(totalPrice);
        return booking;
    }

    // =========================================================
    // calculateCancellation - cannot cancel rules
    // =========================================================

    @Test
    void calculateCancellation_alreadyCancelled_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CANCELLED,
                LocalDate.now().plusDays(10),
                new BigDecimal("400.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("already cancelled"));
    }

    @Test
    void calculateCancellation_checkedIn_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CHECKED_IN,
                LocalDate.now().plusDays(10),
                new BigDecimal("400.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("Cannot cancel after check-in"));
    }

    @Test
    void calculateCancellation_checkedOut_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CHECKED_OUT,
                LocalDate.now().plusDays(10),
                new BigDecimal("400.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("Cannot cancel after check-out"));
    }

    @Test
    void calculateCancellation_checkInDatePassed_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().minusDays(1),
                new BigDecimal("400.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("check-in date has passed"));
    }

    // =========================================================
    // calculateCancellation - refund tiers
    // =========================================================

    @Test
    void calculateCancellation_30PlusDays_returns100Percent() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(30),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.calculateCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(100, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("400.00").compareTo(response.getRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getCancellationFee()));
        assertTrue(response.getPolicyMessage().contains("30+ days"));
        assertEquals(30, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_14To29Days_returns75Percent() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(20),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.calculateCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(75, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("300.00").compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getCancellationFee()));
        assertTrue(response.getPolicyMessage().contains("14-29"));
        assertEquals(20, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_7To13Days_returns50Percent() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(10),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.calculateCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(50, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getCancellationFee()));
        assertTrue(response.getPolicyMessage().contains("7-13"));
        assertEquals(10, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_3To6Days_returns25Percent() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.calculateCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(25, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(response.getCancellationFee()));
        assertTrue(response.getPolicyMessage().contains("3-6"));
        assertEquals(5, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_lessThan3Days_returnsZeroPercent() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(2),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.calculateCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(0, response.getRefundPercentage());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("400.00").compareTo(response.getCancellationFee()));
        assertTrue(response.getPolicyMessage().contains("Less than 3 days"));
        assertEquals(2, response.getDaysUntilCheckIn());
    }

    // =========================================================
    // previewCancellation
    // =========================================================

    @Test
    void previewCancellation_whenAllowed_mirrorsCalculateCancellation() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(14),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.previewCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(75, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("300.00").compareTo(response.getRefundAmount()));
        assertEquals(14, response.getDaysUntilCheckIn());
    }

    @Test
    void previewCancellation_whenNotAllowed_returnsCanCancelFalse() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CANCELLED,
                LocalDate.now().plusDays(10),
                new BigDecimal("400.00")
        );

        CancellationResponse response = service.previewCancellation(booking);

        assertFalse(response.isCanCancel());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("400.00").compareTo(response.getCancellationFee()));
        assertEquals(0, response.getRefundPercentage());
        assertTrue(response.getPolicyMessage().contains("already cancelled"));
        assertEquals(0, response.getDaysUntilCheckIn());
    }

    // =========================================================
    // calculateManagerCancellation
    // =========================================================

    @Test
    void calculateManagerCancellation_30PlusDays_applies10PercentBonus() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(30),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.calculateManagerCancellation(booking);

        assertTrue(response.isCanCancel());
        assertEquals(100, response.getRefundPercentage());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getCancellationFee()));
        assertEquals(0, new BigDecimal("20.00").compareTo(response.getBonusAmount()));
        assertTrue(response.getBonusTierDescription().contains("10%"));
    }

    @Test
    void calculateManagerCancellation_14To29Days_applies25PercentBonus() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(20),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.calculateManagerCancellation(booking);

        assertEquals(0, new BigDecimal("50.00").compareTo(response.getBonusAmount()));
        assertTrue(response.getBonusTierDescription().contains("25%"));
    }

    @Test
    void calculateManagerCancellation_7To13Days_applies35PercentBonus() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(10),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.calculateManagerCancellation(booking);

        assertEquals(0, new BigDecimal("70.00").compareTo(response.getBonusAmount()));
        assertTrue(response.getBonusTierDescription().contains("35%"));
    }

    @Test
    void calculateManagerCancellation_3To6Days_applies40PercentBonus() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.calculateManagerCancellation(booking);

        assertEquals(0, new BigDecimal("80.00").compareTo(response.getBonusAmount()));
        assertTrue(response.getBonusTierDescription().contains("40%"));
    }

    @Test
    void calculateManagerCancellation_lessThan3Days_applies50PercentBonus() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(2),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.calculateManagerCancellation(booking);

        assertEquals(0, new BigDecimal("100.00").compareTo(response.getBonusAmount()));
        assertTrue(response.getBonusTierDescription().contains("50%"));
    }

    @Test
    void calculateManagerCancellation_checkedIn_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CHECKED_IN,
                LocalDate.now().plusDays(10),
                new BigDecimal("200.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateManagerCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("already checked in"));
    }

    @Test
    void calculateManagerCancellation_checkedOut_throws() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CHECKED_OUT,
                LocalDate.now().plusDays(10),
                new BigDecimal("200.00")
        );

        CancellationNotAllowedException ex = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateManagerCancellation(booking)
        );

        assertTrue(ex.getMessage().contains("checked-out"));
    }

    @Test
    void previewManagerCancellation_whenNotAllowed_returnsCanCancelFalse() {
        Booking booking = buildBooking(
                Booking.BookingStatus.CANCELLED,
                LocalDate.now().plusDays(10),
                new BigDecimal("200.00")
        );

        CancellationResponse response = service.previewManagerCancellation(booking);

        assertFalse(response.isCanCancel());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefundAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getCancellationFee()));
        assertEquals(0, response.getRefundPercentage());
    }
}