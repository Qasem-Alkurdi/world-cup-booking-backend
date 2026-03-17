package com.worldcup.hotelbooking.booking.cancellation;

import com.worldcup.hotelbooking.booking.booking.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CancellationPolicyServiceImplTest {

    private final CancellationPolicyServiceImpl service = new CancellationPolicyServiceImpl();

    @Test
    void calculateCancellation_shouldThrow_whenBookingAlreadyCancelled() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CANCELLED, LocalDate.now().plusDays(10), BigDecimal.valueOf(400));

        // Act + Assert
        CancellationNotAllowedException exception = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertEquals("Booking is already cancelled", exception.getMessage());
    }

    @Test
    void calculateCancellation_shouldThrow_whenBookingCheckedIn() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CHECKED_IN, LocalDate.now().plusDays(10), BigDecimal.valueOf(400));

        // Act + Assert
        CancellationNotAllowedException exception = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertEquals("Cannot cancel after check-in. Please contact hotel reception.", exception.getMessage());
    }

    @Test
    void calculateCancellation_shouldThrow_whenBookingCheckedOut() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CHECKED_OUT, LocalDate.now().plusDays(10), BigDecimal.valueOf(400));

        // Act + Assert
        CancellationNotAllowedException exception = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertEquals("Cannot cancel after check-out", exception.getMessage());
    }

    @Test
    void calculateCancellation_shouldThrow_whenCheckInDateHasPassed() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().minusDays(1), BigDecimal.valueOf(400));

        // Act + Assert
        CancellationNotAllowedException exception = assertThrows(
                CancellationNotAllowedException.class,
                () -> service.calculateCancellation(booking)
        );

        assertEquals("Cannot cancel after check-in date has passed", exception.getMessage());
    }

    @Test
    void calculateCancellation_shouldReturnFullRefund_whenCheckInIs30OrMoreDaysAway() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(30), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.calculateCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.valueOf(400), response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.ZERO, response.getCancellationFee());
        assertEquals(100, response.getRefundPercentage());
        assertEquals("Full refund - 30+ days notice", response.getPolicyMessage());
        assertEquals(30, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_shouldReturnSeventyFivePercentRefund_whenCheckInIsBetween14And29DaysAway() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(20), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.calculateCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.valueOf(300), response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(100), response.getCancellationFee());
        assertEquals(75, response.getRefundPercentage());
        assertEquals("75% refund - 14-29 days notice", response.getPolicyMessage());
        assertEquals(20, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_shouldReturnFiftyPercentRefund_whenCheckInIsBetween7And13DaysAway() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(10), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.calculateCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.valueOf(200), response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(200), response.getCancellationFee());
        assertEquals(50, response.getRefundPercentage());
        assertEquals("50% refund - 7-13 days notice", response.getPolicyMessage());
        assertEquals(10, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_shouldReturnTwentyFivePercentRefund_whenCheckInIsBetween3And6DaysAway() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(5), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.calculateCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.valueOf(100), response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(300), response.getCancellationFee());
        assertEquals(25, response.getRefundPercentage());
        assertEquals("25% refund - 3-6 days notice", response.getPolicyMessage());
        assertEquals(5, response.getDaysUntilCheckIn());
    }

    @Test
    void calculateCancellation_shouldReturnNoRefund_whenCheckInIsLessThan3DaysAway() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.calculateCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.ZERO, response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(400), response.getCancellationFee());
        assertEquals(0, response.getRefundPercentage());
        assertEquals("No refund - Less than 3 days notice", response.getPolicyMessage());
        assertEquals(2, response.getDaysUntilCheckIn());
    }

    @Test
    void previewCancellation_shouldReturnBlockedResponse_whenCancellationIsNotAllowed() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CANCELLED, LocalDate.now().plusDays(10), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.previewCancellation(booking);

        // Assert
        assertFalse(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.ZERO, response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(400), response.getCancellationFee());
        assertEquals(0, response.getRefundPercentage());
        assertEquals("Booking is already cancelled", response.getPolicyMessage());
        assertEquals(0, response.getDaysUntilCheckIn());
    }

    @Test
    void previewCancellation_shouldMirrorCalculatedResponse_whenCancellationIsAllowed() {
        // Arrange
        Booking booking = createBooking(Booking.BookingStatus.CONFIRMED, LocalDate.now().plusDays(14), BigDecimal.valueOf(400));

        // Act
        CancellationResponseDto response = service.previewCancellation(booking);

        // Assert
        assertTrue(response.isCanCancel());
        assertBigDecimalEquals(BigDecimal.valueOf(300), response.getRefundAmount());
        assertBigDecimalEquals(BigDecimal.valueOf(100), response.getCancellationFee());
        assertEquals(75, response.getRefundPercentage());
        assertEquals("75% refund - 14-29 days notice", response.getPolicyMessage());
        assertEquals(14, response.getDaysUntilCheckIn());
    }

    private Booking createBooking(Booking.BookingStatus status, LocalDate checkInDate, BigDecimal totalPrice) {
        Booking booking = new Booking();
        booking.setBookingReference("WC-TEST-REF");
        booking.setStatus(status);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkInDate.plusDays(3));
        booking.setTotalPrice(totalPrice);
        return booking;
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                () -> "Expected amount " + expected + " but was " + actual);
    }
}
