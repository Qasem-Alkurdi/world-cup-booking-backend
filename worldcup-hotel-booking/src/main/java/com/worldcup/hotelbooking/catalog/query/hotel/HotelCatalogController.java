package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog/hotels")
@Transactional(readOnly = true)
public class HotelCatalogController {

    private final HotelCatalogServiceImpl service;

    public HotelCatalogController(HotelCatalogServiceImpl service) {
        this.service = service;
    }

    @GetMapping
    public Page<HotelCatalogResponseDto> search(
            @ModelAttribute HotelCatalogCriteria criteria,
            Pageable pageable
    ) {
        return service.search(pageable, criteria);
    }
}
