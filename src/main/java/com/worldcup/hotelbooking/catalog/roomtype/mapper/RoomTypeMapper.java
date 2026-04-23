package com.worldcup.hotelbooking.catalog.roomtype.mapper;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.dto.CreateRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeResponseDto;

public class RoomTypeMapper {

    public static RoomType fromCreate(CreateRoomTypeRequestDto dto) {
        RoomType rt = new RoomType();

        rt.setName(dto.name());
        rt.setDescription(dto.description());

        rt.setMaxAdults(dto.maxAdults());
        rt.setMaxChildren(dto.maxChildren());

        rt.setRoomSizeSqm(dto.roomSizeSqm());
        rt.setBasePrice(dto.basePrice());

        // default currency
        String currency = dto.currency();
        rt.setCurrency(currency != null && !currency.isBlank() ? currency : "USD");

        rt.setTotalRooms(dto.totalRooms());

        // amenities: null -> false
        rt.setHasPrivateBathroom(Boolean.TRUE.equals(dto.hasPrivateBathroom()));
        rt.setHasAirConditioning(Boolean.TRUE.equals(dto.hasAirConditioning()));
        rt.setHasHeating(Boolean.TRUE.equals(dto.hasHeating()));
        rt.setHasBalcony(Boolean.TRUE.equals(dto.hasBalcony()));
        rt.setHasTv(Boolean.TRUE.equals(dto.hasTv()));
        rt.setHasMinibar(Boolean.TRUE.equals(dto.hasMinibar()));
        rt.setHasSafe(Boolean.TRUE.equals(dto.hasSafe()));
        rt.setHasHairdryer(Boolean.TRUE.equals(dto.hasHairdryer()));
        rt.setHasWorkDesk(Boolean.TRUE.equals(dto.hasWorkDesk()));
        rt.setHasSoundproofing(Boolean.TRUE.equals(dto.hasSoundproofing()));
        rt.setHasCoffeeMachine(Boolean.TRUE.equals(dto.hasCoffeeMachine()));

        return rt;
    }

    // PUT/Replace: طبّق على existing (لا تعمل new)
    public static void applyReplace(RoomType rt, ReplaceRoomTypeRequestDto dto) {
        rt.setName(dto.name());
        rt.setDescription(dto.description());

        rt.setMaxAdults(dto.maxAdults());
        rt.setMaxChildren(dto.maxChildren());

        rt.setRoomSizeSqm(dto.roomSizeSqm());
        rt.setBasePrice(dto.basePrice());
        rt.setCurrency(dto.currency());
        rt.setTotalRooms(dto.totalRooms());

        // Replace DTO amenities كلها NotNull
        rt.setHasPrivateBathroom(dto.hasPrivateBathroom());
        rt.setHasAirConditioning(dto.hasAirConditioning());
        rt.setHasHeating(dto.hasHeating());
        rt.setHasBalcony(dto.hasBalcony());
        rt.setHasTv(dto.hasTv());
        rt.setHasMinibar(dto.hasMinibar());
        rt.setHasSafe(dto.hasSafe());
        rt.setHasHairdryer(dto.hasHairdryer());
        rt.setHasWorkDesk(dto.hasWorkDesk());
        rt.setHasSoundproofing(dto.hasSoundproofing());
        rt.setHasCoffeeMachine(dto.hasCoffeeMachine());
    }

    public static RoomTypeResponseDto toResponse(RoomType rt) {
        return new RoomTypeResponseDto(
                rt.getId(),
                rt.getHotel().getId(),

                rt.getName(),
                rt.getDescription(),

                rt.getMaxAdults(),
                rt.getMaxChildren(),

                rt.getRoomSizeSqm(),

                rt.getBasePrice(),
                rt.getCurrency(),

                rt.getTotalRooms(),

                rt.isHasPrivateBathroom(),
                rt.isHasAirConditioning(),
                rt.isHasHeating(),
                rt.isHasBalcony(),
                rt.isHasTv(),
                rt.isHasMinibar(),
                rt.isHasSafe(),
                rt.isHasHairdryer(),
                rt.isHasWorkDesk(),
                rt.isHasSoundproofing(),
                rt.isHasCoffeeMachine(),

                rt.getCreatedAt(),
                rt.getUpdatedAt()
        );
    }
}
