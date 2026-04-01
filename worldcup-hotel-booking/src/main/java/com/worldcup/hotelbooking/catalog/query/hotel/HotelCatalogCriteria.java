package com.worldcup.hotelbooking.catalog.query.hotel;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class HotelCatalogCriteria {
    private String name;
    private String city;
    private String country;

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

    private Long matchId;
    private Long stadiumId;

    private Double latitude;
    private Double longitude;
    private Double maxDistanceKm;
    private Double minDistanceKm;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private BigDecimal minTotalPrice;
    private BigDecimal maxTotalPrice;
    private Integer numberOfRooms;
    private BigDecimal minRating;
    private BigDecimal maxRating;
    private Integer minReviewCount;
}