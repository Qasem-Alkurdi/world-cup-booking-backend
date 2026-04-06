package com.worldcup.hotelbooking.catalog.query.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class HotelCatalogResponseDto {
    private String name;
    private Long id;
    private String description;
    private String city;
    private String country;
    private String primaryPhotoUrl;

    private BigDecimal averageRating;
    private Integer reviewCount;

    private BigDecimal minPrice;
    private Double distanceKm;

    private Boolean hasGym;
    private Boolean hasWifi;
    private Boolean hasParking;
    private Boolean hasBreakfast;
    private Boolean hasAirConditioning;
    private Boolean hasHeating;
    private Boolean hasPool;
    private Boolean hasSpa;
    private Boolean hasElevator;
    private Boolean hasRestaurant;
    private Boolean hasRoomService;
    private Boolean hasLaundry;
    private Boolean hasAirportShuttle;
    private Boolean hasAccessibleFacilities;
    private Boolean petFriendly;
}