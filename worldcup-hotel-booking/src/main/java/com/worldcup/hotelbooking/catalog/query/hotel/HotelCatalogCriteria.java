package com.worldcup.hotelbooking.catalog.query.hotel;

import lombok.Getter;
import lombok.Setter;

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
    private Double latitude;
    private Double longitude;
    private Double maxDistanceKm;
    private Double minDistanceKm;

}
