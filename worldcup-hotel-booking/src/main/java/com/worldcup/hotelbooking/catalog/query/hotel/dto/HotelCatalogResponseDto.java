package com.worldcup.hotelbooking.catalog.query.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HotelCatalogResponseDto {
    private String name;
    private Long id;
    private String description;
    private String city;
    private String country;
}
