package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import org.springframework.data.domain.Pageable;

public interface HotelCatalogService {
    HotelCatalogSearchResponseDto search(Pageable pageable, HotelCatalogCriteria criteria);
}
