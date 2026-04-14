package com.worldcup.hotelbooking.catalog.hotel.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.dto.CreateHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.HotelResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.ReplaceHotelRequestDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class HotelMapper {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private static Point point(Double latitude, Double longitude) {
        // JTS: x = longitude, y = latitude
        return GF.createPoint(new Coordinate(longitude, latitude));
    }

    public static Hotel fromCreate(CreateHotelRequestDto dto) {
        Hotel h = new Hotel();
        h.setName(dto.name());
        h.setDescription(dto.description());
        h.setContactEmail(dto.contactEmail());
        h.setContactPhone(dto.contactPhone());
        h.setCountry(dto.country());
        h.setCity(dto.city());
        h.setAddressLine(dto.addressLine());
        h.setLocation(point(dto.latitude(), dto.longitude()));

        // amenities (Boolean -> boolean with default false when null)
        h.setHasWifi(Boolean.TRUE.equals(dto.hasWifi()));
        h.setHasParking(Boolean.TRUE.equals(dto.hasParking()));
        h.setHasBreakfast(Boolean.TRUE.equals(dto.hasBreakfast()));
        h.setHasAirConditioning(Boolean.TRUE.equals(dto.hasAirConditioning()));
        h.setHasHeating(Boolean.TRUE.equals(dto.hasHeating()));
        h.setHasElevator(Boolean.TRUE.equals(dto.hasElevator()));
        h.setHasRestaurant(Boolean.TRUE.equals(dto.hasRestaurant()));
        h.setHasRoomService(Boolean.TRUE.equals(dto.hasRoomService()));
        h.setHasGym(Boolean.TRUE.equals(dto.hasGym()));
        h.setHasPool(Boolean.TRUE.equals(dto.hasPool()));
        h.setHasSpa(Boolean.TRUE.equals(dto.hasSpa()));
        h.setHasLaundry(Boolean.TRUE.equals(dto.hasLaundry()));
        h.setHasAirportShuttle(Boolean.TRUE.equals(dto.hasAirportShuttle()));
        h.setHasAccessibleFacilities(Boolean.TRUE.equals(dto.hasAccessibleFacilities()));
        h.setPetFriendly(Boolean.TRUE.equals(dto.petFriendly()));

        return h;
    }

    public static Hotel fromReplace(ReplaceHotelRequestDto dto) {
        Hotel h = new Hotel();
        applyReplace(dto, h);
        return h;
    }

    public static void applyReplace(ReplaceHotelRequestDto dto, Hotel h) {
        h.setName(dto.name());
        h.setDescription(dto.description());
        h.setContactEmail(dto.contactEmail());
        h.setContactPhone(dto.contactPhone());
        h.setCountry(dto.country());
        h.setCity(dto.city());
        h.setAddressLine(dto.addressLine());
        h.setLocation(point(dto.latitude(), dto.longitude()));

        h.setHasWifi(dto.hasWifi());
        h.setHasParking(dto.hasParking());
        h.setHasBreakfast(dto.hasBreakfast());
        h.setHasAirConditioning(dto.hasAirConditioning());
        h.setHasHeating(dto.hasHeating());
        h.setHasElevator(dto.hasElevator());
        h.setHasRestaurant(dto.hasRestaurant());
        h.setHasRoomService(dto.hasRoomService());
        h.setHasGym(dto.hasGym());
        h.setHasPool(dto.hasPool());
        h.setHasSpa(dto.hasSpa());
        h.setHasLaundry(dto.hasLaundry());
        h.setHasAirportShuttle(dto.hasAirportShuttle());
        h.setHasAccessibleFacilities(dto.hasAccessibleFacilities());
        h.setPetFriendly(dto.petFriendly());
    }

    public static HotelResponseDto toResponse(Hotel h) {
        Double lat = h.getLatitude();
        Double lng = h.getLongitude();

        // Fallback to location point if generated columns are null
        if ((lat == null || lng == null) && h.getLocation() != null) {
            lat = h.getLocation().getY(); // Y is latitude
            lng = h.getLocation().getX(); // X is longitude
        }

        return new HotelResponseDto(
                h.getId(),
                h.getOwner() != null ? h.getOwner().getId() : null,
                h.getName(),
                h.getDescription(),
                h.getContactEmail(),
                h.getContactPhone(),
                h.getCountry(),
                h.getCity(),
                h.getAddressLine(),
                lat,
                lng,
                h.getStatus() != null ? h.getStatus().name() : null,
                h.isHasWifi(),
                h.isHasParking(),
                h.isHasBreakfast(),
                h.isHasAirConditioning(),
                h.isHasHeating(),
                h.isHasElevator(),
                h.isHasRestaurant(),
                h.isHasRoomService(),
                h.isHasGym(),
                h.isHasPool(),
                h.isHasSpa(),
                h.isHasLaundry(),
                h.isHasAirportShuttle(),
                h.isHasAccessibleFacilities(),
                h.isPetFriendly(),
                h.getAverageRating(),
                h.getReviewCount(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
