package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.review.dto.CreateReviewRequestDto;
import com.worldcup.hotelbooking.review.dto.HotelReviewSummaryDto;
import com.worldcup.hotelbooking.review.dto.ReviewResponseDto;
import com.worldcup.hotelbooking.review.dto.UpdateReviewRequestDto;
import com.worldcup.hotelbooking.review.mapper.ReviewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Review Controller", description = "APIs for managing hotel reviews")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create review for booking",
            description = "Creates a review for a checked-out booking. Only the booking owner can create one review per booking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @PostMapping("/bookings/{bookingId}/review")
    @PreAuthorize("hasRole('ADMIN') or @reviewAuthorizationService.canCreateReview(#bookingId, authentication)")
    public ResponseEntity<ReviewResponseDto> create(
            @Parameter(description = "Booking id", example = "1")
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateReviewRequestDto body
    ) {
        Review saved = service.create(bookingId, body);
        return ResponseEntity.ok(ReviewMapper.toResponse(saved));
    }

    @Operation(
            summary = "Update review",
            description = "Updates the current user's review."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review updated successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @PatchMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @reviewAuthorizationService.canManageReview(#reviewId, authentication)")
    public ResponseEntity<ReviewResponseDto> update(
            @Parameter(description = "Review id", example = "1")
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequestDto body
    ) {
        Review updated = service.update(reviewId, body);
        return ResponseEntity.ok(ReviewMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete review",
            description = "Deletes the current user's review."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @reviewAuthorizationService.canManageReview(#reviewId, authentication)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Review id", example = "1")
            @PathVariable Long reviewId
    ) {
        service.delete(reviewId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get hotel reviews",
            description = "Returns visible reviews for a hotel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping("/hotels/{hotelId}/reviews")
    public List<ReviewResponseDto> getHotelReviews(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId
    ) {
        return service.getHotelReviews(hotelId)
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Get hotel review summary",
            description = "Returns average rating and review count for a hotel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping("/hotels/{hotelId}/reviews/summary")
    public ResponseEntity<HotelReviewSummaryDto> getSummary(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(service.getHotelReviewSummary(hotelId));
    }
}