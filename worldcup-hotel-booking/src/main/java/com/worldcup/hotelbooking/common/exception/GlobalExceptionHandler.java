package com.worldcup.hotelbooking.common.exception;

import com.worldcup.hotelbooking.auth.InvalidCredentialsException;
import com.worldcup.hotelbooking.auth.InvalidRefreshTokenException;
import com.worldcup.hotelbooking.availability_pricing.match.MatchNotFoundException;
import com.worldcup.hotelbooking.availability_pricing.stadium.StadiumNotFoundException;
import com.worldcup.hotelbooking.booking.booking.BookingNotFoundException;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomNotFoundException;
import com.worldcup.hotelbooking.catalog.hotel.exception.DeleteConflictException;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.HotelPhotoNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.InvalidPhotoOrderException;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeAlreadyExistsException;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtypephoto.exception.RoomTypePhotoNotFoundException;
import com.worldcup.hotelbooking.catalog.storage.exception.InvalidPhotoFileException;
import com.worldcup.hotelbooking.catalog.storage.exception.StorageOperationException;
import com.worldcup.hotelbooking.payment.PaymentException;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // Common helpers
    // =========================

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    private String firstNonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }


    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentException ex, HttpServletRequest request) {
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


// catalog start
    // =========================
    // Catalog - Hotel
    // =========================

    @ExceptionHandler(HotelNotFoundException.class)
    public ResponseEntity<ApiError> handleHotelNotFound(
            HotelNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                firstNonBlank(ex.getMessage(), "The requested hotel was not found."),
                request
        );
    }

    @ExceptionHandler(DeleteConflictException.class)
    public ResponseEntity<ApiError> handleDeleteConflict(
            DeleteConflictException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                firstNonBlank(ex.getMessage(), "This hotel cannot be deleted at the moment."),
                request
        );
    }

    // =========================
    // Catalog - Room Type
    // =========================

    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ResponseEntity<ApiError> handleRoomTypeNotFound(
            RoomTypeNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                firstNonBlank(ex.getMessage(), "The requested room type was not found."),
                request
        );
    }

    @ExceptionHandler(RoomTypeAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleRoomTypeAlreadyExists(
            RoomTypeAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                firstNonBlank(ex.getMessage(), "A room type with the same name already exists for this hotel."),
                request
        );
    }

    // =========================
    // Catalog - Search / Query
    // =========================

    @ExceptionHandler(CheckOutBeforeCheckIn.class)
    public ResponseEntity<ApiError> handleCheckOutBeforeCheckIn(
            CheckOutBeforeCheckIn ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Check-out date must be after check-in date.",
                request
        );
    }

    @ExceptionHandler(CheckOutDateAreRequired.class)
    public ResponseEntity<ApiError> handleCheckOutDateAreRequired(
            CheckOutDateAreRequired ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Check-out date is required when check-in date is provided.",
                request
        );
    }

    // =========================
    // Catalog - Photos / Storage
    // =========================

    @ExceptionHandler(InvalidPhotoFileException.class)
    public ResponseEntity<ApiError> handleInvalidPhotoFileException(
            InvalidPhotoFileException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                firstNonBlank(
                        ex.getMessage(),
                        "The uploaded photo is invalid. Please upload a supported image file."
                ),
                request
        );
    }

    @ExceptionHandler(StorageOperationException.class)
    public ResponseEntity<ApiError> handleStorageOperationException(
            StorageOperationException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An error occurred while processing the file. Please try again later.",
                request
        );
    }

    @ExceptionHandler(HotelPhotoNotFoundException.class)
    public ResponseEntity<ApiError> handleHotelPhotoNotFound(
            HotelPhotoNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                firstNonBlank(ex.getMessage(), "The requested hotel photo was not found."),
                request
        );
    }

    @ExceptionHandler(RoomTypePhotoNotFoundException.class)
    public ResponseEntity<ApiError> handleRoomTypePhotoNotFound(
            RoomTypePhotoNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                firstNonBlank(ex.getMessage(), "The requested room type photo was not found."),
                request
        );
    }

    @ExceptionHandler(InvalidPhotoOrderException.class)
    public ResponseEntity<ApiError> handleInvalidPhotoOrderException(
            InvalidPhotoOrderException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                firstNonBlank(
                        ex.getMessage(),
                        "The provided photo order is invalid."
                ),
                request
        );
    }

    // =========================
    // Validation
    // =========================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Request validation failed. Please check the submitted data.";
        }

        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Request validation failed. Please check the submitted parameters.";
        }

        return error(HttpStatus.BAD_REQUEST, message, request);
    }


    // =========================
    // Request body / JSON / Multipart
    // =========================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "The request body is invalid or contains unsupported field values.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Missing required request part: " + ex.getRequestPartName() + ".",
                request
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipartException(
            MultipartException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "The uploaded form data is invalid. Please verify the file upload request.",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded file is too large.",
                request
        );
    }

    // =========================
    // Security
    // =========================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.",
                request
        );
    }

    // =========================
    // Fallback
    // =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request
        );
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
        String exMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (exMessage.contains("email")) {
            message = "Email already registered. Please use a different email address.";
        } else if (exMessage.contains("username")) {
            message = "Username already taken. Please choose a different username.";
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


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                401,
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                401,
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    //user end


    //stadium start
    @ExceptionHandler(StadiumNotFoundException.class)
    public ResponseEntity<ApiError> handleStadiumNotFound(StadiumNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                409,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    //stadium end
    //Match start

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<ApiError> handleMatchNotFound(MatchNotFoundException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    //match end
}
