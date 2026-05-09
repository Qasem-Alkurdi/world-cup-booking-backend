package com.worldcup.hotelbooking.catalog.print;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;

public class CatalogPrintableFactory {

    private CatalogPrintableFactory() {
    }

    public static CatalogComponent fromHotel(Hotel hotel) {

        CatalogComposite hotelPrintable = new HotelPrintable(hotel);

        for (RoomType roomType : hotel.getRoomTypes()) {
            hotelPrintable.add(
                    new RoomTypePrintable(roomType)
            );
        }

        return hotelPrintable;
    }

    public static CatalogLeaf fromRoomType(RoomType roomType) {

        return new RoomTypePrintable(roomType) {
        };
    }
}