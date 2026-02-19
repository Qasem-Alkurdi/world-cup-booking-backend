package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelCatalogService {
    Page<HotelCatalogResponseDto> search(Pageable pageable, HotelCatalogCriteria criteria);
}
