package com.worldcup.hotelbooking.common.exception;

import com.worldcup.hotelbooking.booking.booking.BookingNotFoundException;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomNotFoundException;
import com.worldcup.hotelbooking.catalog.hotel.exceptions.DeleteConflictException;
import com.worldcup.hotelbooking.catalog.hotel.exceptions.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.exceptions.RoomTypeAlreadyExistsException;
import com.worldcup.hotelbooking.catalog.roomtype.exceptions.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.payment.payment.PaymentNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    //For Booking
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiError> handleBookingNotFound(BookingNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                400,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BookingRoomNotFoundException.class)
    public ResponseEntity<ApiError> handleBookingRoomNotFound(BookingRoomNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Validation failed");

        ApiError body = new ApiError(
                Instant.now().toString(),
                400,
                "Bad Request",
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
// catalog start

    // hotel start
    // 404
    @ExceptionHandler(HotelNotFoundException.class)
    public ResponseEntity<ApiError> handleHotelNotFound(HotelNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 409
    @ExceptionHandler(DeleteConflictException.class)
    public ResponseEntity<ApiError> handleDeleteConflict(DeleteConflictException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                409,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    // hotel end

    //roomType start
    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ResponseEntity<ApiError> handleRoomTypeNotFound(
            RoomTypeNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RoomTypeAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleRoomTypeAlreadyExists(
            RoomTypeAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                409,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

// catalog end

    //user start
    @ExceptionHandler(AppUserNotFoundException.class)
    public ResponseEntity<ApiError> handleAppUserNotFound(AppUserNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String message = "Duplicate entry detected.";

        // Check if the violation is about the email column
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("email")) {
            message = "Email already registered. Please use a different email address.";
        } else {
            // For other integrity violations (if any), you might want a generic message
            message = "Data integrity violation.";
        }

        ApiError body = new ApiError(
                Instant.now().toString(),
                409,
                "Conflict",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    //user end
}
