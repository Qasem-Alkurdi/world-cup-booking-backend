package com.worldcup.hotelbooking.catalog.print;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;

public class RoomTypePrintable implements CatalogLeaf {

    private final RoomType roomType;

    public RoomTypePrintable(RoomType roomType) {
        this.roomType = roomType;
    }

    @Override
    public String print() {

        return """
                
                Room Type:
                ------------------------
                Name: %s
                Description: %s
                Capacity:
                  - Adults: %d
                  - Children: %d
                
                Price:
                  - Base Price: %s %s
                
                Total Rooms: %d
                
                Amenities:
                  - Private Bathroom: %b
                  - Air Conditioning: %b
                  - Heating: %b
                  - Balcony: %b
                  - TV: %b
                  - Minibar: %b
                  - Safe: %b
                  - Hairdryer: %b
                  - Work Desk: %b
                  - Soundproofing: %b
                  - Coffee Machine: %b
                """.formatted(
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
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