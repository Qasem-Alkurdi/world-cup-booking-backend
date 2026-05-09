package com.worldcup.hotelbooking.catalog.print;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;

import java.util.ArrayList;
import java.util.List;

public class HotelPrintable implements CatalogComposite {

    private final Hotel hotel;

    private final List<CatalogLeaf> children = new ArrayList<>();

    public HotelPrintable(Hotel hotel) {
        this.hotel = hotel;
    }

    @Override
    public void add(CatalogLeaf leaf) {
        children.add(leaf);
    }

    @Override
    public void remove(CatalogLeaf leaf) {
        children.remove(leaf);
    }

    @Override
    public String print() {

        StringBuilder builder = new StringBuilder();

        builder.append("""
                
                ======================================
                           HOTEL DETAILS
                ======================================
                
                Hotel Name: %s
                
                Description: %s
                
                Location:
                  - Country: %s
                  - City: %s
                  - Address: %s
                
                Contact:
                  - Email: %s
                  - Phone: %s
                
                Rating:
                  - Average Rating: %s
                  - Review Count: %d
                
                Amenities:
                  - Wifi: %b
                  - Parking: %b
                  - Breakfast: %b
                  - Air Conditioning: %b
                  - Heating: %b
                  - Elevator: %b
                  - Restaurant: %b
                  - Room Service: %b
                  - Gym: %b
                  - Pool: %b
                  - Spa: %b
                  - Laundry: %b
                  - Airport Shuttle: %b
                  - Accessible Facilities: %b
                  - Pet Friendly: %b
                
                ======================================
                           ROOM TYPES
                ======================================
                """.formatted(
                hotel.getName(),
                hotel.getDescription(),
                hotel.getCountry(),
                hotel.getCity(),
                hotel.getAddressLine(),
                hotel.getContactEmail(),
                hotel.getContactPhone(),
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
                hotel.isPetFriendly()
        ));

        for (CatalogLeaf leaf : children) {
            builder.append(leaf.print());
            builder.append("\n");
        }

        return builder.toString();
    }
}