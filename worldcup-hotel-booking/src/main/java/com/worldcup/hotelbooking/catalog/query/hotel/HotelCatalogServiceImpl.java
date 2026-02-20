package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelCatalogServiceImpl implements HotelCatalogService {
    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("id", "name", "city");
    private final HotelRepository hotelRepository;

    HotelCatalogServiceImpl(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override

    public Page<HotelCatalogResponseDto> search(Pageable pageable, HotelCatalogCriteria criteria) {
        validateSortFields(pageable);

        Specification<Hotel> distanceSpec = null;

        if (criteria.getMinDistanceKm() != null && criteria.getMaxDistanceKm() != null) {
            distanceSpec = HotelCatalogSpecifications.betweenDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMinDistanceKm(),
                    criteria.getMaxDistanceKm()
            );
        } else if (criteria.getMaxDistanceKm() != null) {
            distanceSpec = HotelCatalogSpecifications.withinDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMaxDistanceKm()
            );
        }

        Specification<Hotel> spec = Specification
                .where(HotelCatalogSpecifications.notDeleted())
                .and(HotelCatalogSpecifications.nameContains(criteria.getName()))
                .and(HotelCatalogSpecifications.cityContains(criteria.getCity()))
                .and(HotelCatalogSpecifications.countryContains(criteria.getCountry()))
                .and(HotelCatalogSpecifications.hasGym(criteria.getHasGym()))
                .and(HotelCatalogSpecifications.hasWifi(criteria.getHasWifi()))
                .and(HotelCatalogSpecifications.hasParking(criteria.getHasParking()))
                .and(HotelCatalogSpecifications.hasBreakfast(criteria.getHasBreakfast()))
                .and(HotelCatalogSpecifications.hasAirConditioning(criteria.getHasAirConditioning()))
                .and(HotelCatalogSpecifications.hasHeating(criteria.getHasHeating()))
                .and(HotelCatalogSpecifications.hasPool(criteria.getHasPool()))
                .and(HotelCatalogSpecifications.hasSpa(criteria.getHasSpa()))
                .and(HotelCatalogSpecifications.hasRestaurant(criteria.getHasRestaurant()))
                .and(HotelCatalogSpecifications.hasRoomService(criteria.getHasRoomService()))
                .and(HotelCatalogSpecifications.hasLaundry(criteria.getHasLaundry()))
                .and(HotelCatalogSpecifications.hasAirportShuttle(criteria.getHasAirportShuttle()))
                .and(HotelCatalogSpecifications.hasAccessibleFacilities(criteria.getHasAccessibleFacilities()))
                .and(distanceSpec)
                .and(HotelCatalogSpecifications.petFriendly(criteria.getPetFriendly()).and(HotelCatalogSpecifications.hasElevator(criteria.getHasElevator())));

        Page<Hotel> result = hotelRepository.findAll(spec, pageable);

        List<HotelCatalogResponseDto> content = result.getContent().stream()
                .map(HotelCatalogMapper::toDto)
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }


    private void validateSortFields(Pageable pageable) {

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty()
                                + ". Allowed fields: " + ALLOWED_SORT_FIELDS
                );
            }
        }
    }


}
