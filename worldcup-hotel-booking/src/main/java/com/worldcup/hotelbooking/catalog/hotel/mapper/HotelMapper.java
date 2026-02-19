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
        h.setName(dto.getName());
        h.setDescription(dto.getDescription());
        h.setContactEmail(dto.getContactEmail());
        h.setContactPhone(dto.getContactPhone());
        h.setCountry(dto.getCountry());
        h.setCity(dto.getCity());
        h.setAddressLine(dto.getAddressLine());
        h.setLocation(point(dto.getLatitude(), dto.getLongitude()));

        // amenities (Boolean -> boolean with default false when null)
        h.setHasWifi(Boolean.TRUE.equals(dto.getHasWifi()));
        h.setHasParking(Boolean.TRUE.equals(dto.getHasParking()));
        h.setHasBreakfast(Boolean.TRUE.equals(dto.getHasBreakfast()));
        h.setHasAirConditioning(Boolean.TRUE.equals(dto.getHasAirConditioning()));
        h.setHasHeating(Boolean.TRUE.equals(dto.getHasHeating()));
        h.setHasElevator(Boolean.TRUE.equals(dto.getHasElevator()));
        h.setHasRestaurant(Boolean.TRUE.equals(dto.getHasRestaurant()));
        h.setHasRoomService(Boolean.TRUE.equals(dto.getHasRoomService()));
        h.setHasGym(Boolean.TRUE.equals(dto.getHasGym()));
        h.setHasPool(Boolean.TRUE.equals(dto.getHasPool()));
        h.setHasSpa(Boolean.TRUE.equals(dto.getHasSpa()));
        h.setHasLaundry(Boolean.TRUE.equals(dto.getHasLaundry()));
        h.setHasAirportShuttle(Boolean.TRUE.equals(dto.getHasAirportShuttle()));
        h.setHasAccessibleFacilities(Boolean.TRUE.equals(dto.getHasAccessibleFacilities()));
        h.setPetFriendly(Boolean.TRUE.equals(dto.getPetFriendly()));

        return h;
    }

    public static Hotel fromReplace(ReplaceHotelRequestDto dto) {
        Hotel h = new Hotel();
        applyReplace(dto, h);
        return h;
    }

    public static void applyReplace(ReplaceHotelRequestDto dto, Hotel h) {
        h.setName(dto.getName());
        h.setDescription(dto.getDescription());
        h.setContactEmail(dto.getContactEmail());
        h.setContactPhone(dto.getContactPhone());
        h.setCountry(dto.getCountry());
        h.setCity(dto.getCity());
        h.setAddressLine(dto.getAddressLine());
        h.setLocation(point(dto.getLatitude(), dto.getLongitude()));

        h.setHasWifi(dto.getHasWifi());
        h.setHasParking(dto.getHasParking());
        h.setHasBreakfast(dto.getHasBreakfast());
        h.setHasAirConditioning(dto.getHasAirConditioning());
        h.setHasHeating(dto.getHasHeating());
        h.setHasElevator(dto.getHasElevator());
        h.setHasRestaurant(dto.getHasRestaurant());
        h.setHasRoomService(dto.getHasRoomService());
        h.setHasGym(dto.getHasGym());
        h.setHasPool(dto.getHasPool());
        h.setHasSpa(dto.getHasSpa());
        h.setHasLaundry(dto.getHasLaundry());
        h.setHasAirportShuttle(dto.getHasAirportShuttle());
        h.setHasAccessibleFacilities(dto.getHasAccessibleFacilities());
        h.setPetFriendly(dto.getPetFriendly());
    }

    public static HotelResponseDto toResponse(Hotel h) {
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
                h.getLatitude(),
                h.getLongitude(),
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
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
