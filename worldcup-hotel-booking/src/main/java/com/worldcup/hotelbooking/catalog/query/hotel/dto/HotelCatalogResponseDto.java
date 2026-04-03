package com.worldcup.hotelbooking.catalog.query.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class HotelCatalogResponseDto {
    private Long id;
    private String name;
    private String description;
    private String city;
    private String country;
    private String primaryPhotoUrl;

    private BigDecimal startingPrice;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Double distanceKm;
}