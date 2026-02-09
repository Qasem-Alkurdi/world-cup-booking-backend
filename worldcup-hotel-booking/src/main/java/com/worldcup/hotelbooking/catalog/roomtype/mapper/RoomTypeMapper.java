package com.worldcup.hotelbooking.catalog.roomtype.mapper;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.dto.CreateRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeResponseDto;

public class RoomTypeMapper {

    public static RoomType fromCreate(CreateRoomTypeRequestDto dto) {
        RoomType rt = new RoomType();

        rt.setName(dto.getName());
        rt.setDescription(dto.getDescription());

        rt.setMaxAdults(dto.getMaxAdults());
        rt.setMaxChildren(dto.getMaxChildren());

        rt.setRoomSizeSqm(dto.getRoomSizeSqm());
        rt.setBasePrice(dto.getBasePrice());

        // default currency
        String currency = dto.getCurrency();
        rt.setCurrency(currency != null && !currency.isBlank() ? currency : "USD");

        rt.setTotalRooms(dto.getTotalRooms());

        // amenities: null -> false
        rt.setHasPrivateBathroom(Boolean.TRUE.equals(dto.getHasPrivateBathroom()));
        rt.setHasAirConditioning(Boolean.TRUE.equals(dto.getHasAirConditioning()));
        rt.setHasHeating(Boolean.TRUE.equals(dto.getHasHeating()));
        rt.setHasBalcony(Boolean.TRUE.equals(dto.getHasBalcony()));
        rt.setHasTv(Boolean.TRUE.equals(dto.getHasTv()));
        rt.setHasMinibar(Boolean.TRUE.equals(dto.getHasMinibar()));
        rt.setHasSafe(Boolean.TRUE.equals(dto.getHasSafe()));
        rt.setHasHairdryer(Boolean.TRUE.equals(dto.getHasHairdryer()));
        rt.setHasWorkDesk(Boolean.TRUE.equals(dto.getHasWorkDesk()));
        rt.setHasSoundproofing(Boolean.TRUE.equals(dto.getHasSoundproofing()));
        rt.setHasCoffeeMachine(Boolean.TRUE.equals(dto.getHasCoffeeMachine()));

        return rt;
    }

    // PUT/Replace: طبّق على existing (لا تعمل new)
    public static void applyReplace(RoomType rt, ReplaceRoomTypeRequestDto dto) {
        rt.setName(dto.getName());
        rt.setDescription(dto.getDescription());

        rt.setMaxAdults(dto.getMaxAdults());
        rt.setMaxChildren(dto.getMaxChildren());

        rt.setRoomSizeSqm(dto.getRoomSizeSqm());
        rt.setBasePrice(dto.getBasePrice());
        rt.setCurrency(dto.getCurrency());
        rt.setTotalRooms(dto.getTotalRooms());

        // Replace DTO amenities كلها NotNull
        rt.setHasPrivateBathroom(dto.getHasPrivateBathroom());
        rt.setHasAirConditioning(dto.getHasAirConditioning());
        rt.setHasHeating(dto.getHasHeating());
        rt.setHasBalcony(dto.getHasBalcony());
        rt.setHasTv(dto.getHasTv());
        rt.setHasMinibar(dto.getHasMinibar());
        rt.setHasSafe(dto.getHasSafe());
        rt.setHasHairdryer(dto.getHasHairdryer());
        rt.setHasWorkDesk(dto.getHasWorkDesk());
        rt.setHasSoundproofing(dto.getHasSoundproofing());
        rt.setHasCoffeeMachine(dto.getHasCoffeeMachine());
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
