package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/hotels")
@Tag(name = "Hotel Catalog Controller", description = "APIs for searching and browsing hotel catalog")
@Validated
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
            @ApiResponse(responseCode = "200", description = "Hotel catalog retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters")
    })
    @GetMapping
    public HotelCatalogSearchResponseDto search(
            @Parameter(description = "Hotel catalog search criteria")
            @ModelAttribute HotelCatalogCriteria criteria,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size must be greater than or equal to 1")
            int size,

            @RequestParam(required = false)
            String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return service.search(pageable, criteria);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.asc("id"));
        }

        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        Sort.Direction sortDirection;
        if (direction.equalsIgnoreCase("asc")) {
            sortDirection = Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Invalid sort direction: " + direction + ". Allowed: asc, desc");
        }

        return Sort.by(new Sort.Order(sortDirection, property));
    }
}