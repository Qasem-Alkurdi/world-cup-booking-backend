package com.worldcup.hotelbooking.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateReviewRequestDto {

    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}