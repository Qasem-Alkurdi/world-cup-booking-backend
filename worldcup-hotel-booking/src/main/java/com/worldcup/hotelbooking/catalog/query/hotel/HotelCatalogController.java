package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog/hotels")
@Tag(name = "Hotel Catalog Controller", description = "APIs for searching and browsing hotel catalog")
public class HotelCatalogController {

    private final HotelCatalogService service;

    public HotelCatalogController(HotelCatalogService service) {
        this.service = service;
    }

    @Operation(
            summary = "Search hotel catalog",
            description = "Searches hotels in the catalog using filter criteria and pagination"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel catalog retrieved successfully")
    })
    @GetMapping
    public Page<HotelCatalogResponseDto> search(
            @Parameter(description = "Hotel catalog search criteria")
            @ModelAttribute HotelCatalogCriteria criteria,
            @Parameter(description = "Pagination information")
            Pageable pageable
    ) {
        return service.search(pageable, criteria);
    }
}