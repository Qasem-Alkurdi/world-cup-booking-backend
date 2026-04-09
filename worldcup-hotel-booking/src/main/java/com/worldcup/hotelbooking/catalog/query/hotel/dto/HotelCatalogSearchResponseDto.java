package com.worldcup.hotelbooking.catalog.query.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class HotelCatalogSearchResponseDto {
    private Page<HotelCatalogResponseDto> hotels;
    private HotelCatalogSearchMode searchMode;
    private boolean fallbackApplied;
    private String message;
    private BigDecimal maxTotalPriceFound;
}