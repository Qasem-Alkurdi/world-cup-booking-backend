package com.worldcup.hotelbooking.catalog.print.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.print.dto.HotelCatalogCompositeResponseDto;
import com.worldcup.hotelbooking.catalog.print.dto.RoomTypeCatalogLeafResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;

public class HotelCatalogCompositeMapper {

    private HotelCatalogCompositeMapper() {
    }

    public static HotelCatalogCompositeResponseDto toCompositeResponse(Hotel hotel) {
        return new HotelCatalogCompositeResponseDto(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                hotel.getContactEmail(),
                hotel.getContactPhone(),
                hotel.getCountry(),
                hotel.getCity(),
                hotel.getAddressLine(),
                hotel.getAverageRating(),
                hotel.getReviewCount(),
                hotel.isHasWifi(),
                hotel.isHasParking(),
                hotel.isHasBreakfast(),
                hotel.isHasAirConditioning(),
                hotel.isHasHeating(),
                hotel.isHasElevator(),
                hotel.isHasRestaurant(),
                hotel.isHasRoomService(),
                hotel.isHasGym(),
                hotel.isHasPool(),
                hotel.isHasSpa(),
                hotel.isHasLaundry(),
                hotel.isHasAirportShuttle(),
                hotel.isHasAccessibleFacilities(),
                hotel.isPetFriendly(),
                hotel.getRoomTypes()
                        .stream()
                        .map(HotelCatalogCompositeMapper::toLeafResponse)
                        .toList()
        );
    }

    private static RoomTypeCatalogLeafResponseDto toLeafResponse(RoomType roomType) {
        return new RoomTypeCatalogLeafResponseDto(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
                roomType.getRoomSizeSqm(),
                roomType.getBasePrice(),
                roomType.getCurrency(),
                roomType.getTotalRooms(),
                roomType.isHasPrivateBathroom(),
                roomType.isHasAirConditioning(),
                roomType.isHasHeating(),
                roomType.isHasBalcony(),
                roomType.isHasTv(),
                roomType.isHasMinibar(),
                roomType.isHasSafe(),
                roomType.isHasHairdryer(),
                roomType.isHasWorkDesk(),
                roomType.isHasSoundproofing(),
                roomType.isHasCoffeeMachine()
        );
    }
}