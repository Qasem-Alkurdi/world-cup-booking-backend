package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(5)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class RoomTypeSeeder implements CommandLineRunner {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (roomTypeRepository.count() > 0) {
            log.info("Room types already exist. Skipping room type seeder.");
            return;
        }

        List<Hotel> hotels = hotelRepository.findAll();
        if (hotels.isEmpty()) {
            log.warn("No hotels found. Skipping room type seeder.");
            return;
        }

        List<RoomType> roomTypes = new ArrayList<>();

        for (Hotel hotel : hotels) {
            HotelPricingProfile profile = resolveProfile(hotel);

            roomTypes.add(createRoom(
                    hotel,
                    profile.standardName(),
                    standardDescription(hotel),
                    2, 1,
                    profile.standardSizeSqm(),
                    profile.standardBasePrice(),
                    18,
                    true, true, true, false, true, false, true, true, true, true, false
            ));

            roomTypes.add(createRoom(
                    hotel,
                    profile.deluxeName(),
                    deluxeDescription(hotel),
                    2, 2,
                    profile.deluxeSizeSqm(),
                    profile.deluxeBasePrice(),
                    12,
                    true, true, true, true, true, true, true, true, true, true, true
            ));

            roomTypes.add(createRoom(
                    hotel,
                    profile.suiteName(),
                    suiteDescription(hotel),
                    4, 2,
                    profile.suiteSizeSqm(),
                    profile.suiteBasePrice(),
                    8,
                    true, true, true, true, true, true, true, true, true, true, true
            ));
        }

        roomTypeRepository.saveAll(roomTypes);
        log.info("Seeded {} room types for {} hotels.", roomTypes.size(), hotels.size());
    }

    private RoomType createRoom(
            Hotel hotel,
            String name,
            String description,
            int maxAdults,
            int maxChildren,
            BigDecimal roomSizeSqm,
            BigDecimal basePrice,
            int totalRooms,
            boolean hasPrivateBathroom,
            boolean hasAirConditioning,
            boolean hasHeating,
            boolean hasBalcony,
            boolean hasTv,
            boolean hasMinibar,
            boolean hasSafe,
            boolean hasHairdryer,
            boolean hasWorkDesk,
            boolean hasSoundproofing,
            boolean hasCoffeeMachine
    ) {
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setDescription(description);

        roomType.setMaxAdults(maxAdults);
        roomType.setMaxChildren(maxChildren);
        roomType.setRoomSizeSqm(roomSizeSqm);
        roomType.setBasePrice(basePrice);
        roomType.setCurrency("USD");
        roomType.setTotalRooms(totalRooms);

        roomType.setHasPrivateBathroom(hasPrivateBathroom);
        roomType.setHasAirConditioning(hasAirConditioning);
        roomType.setHasHeating(hasHeating);
        roomType.setHasBalcony(hasBalcony);
        roomType.setHasTv(hasTv);
        roomType.setHasMinibar(hasMinibar);
        roomType.setHasSafe(hasSafe);
        roomType.setHasHairdryer(hasHairdryer);
        roomType.setHasWorkDesk(hasWorkDesk);
        roomType.setHasSoundproofing(hasSoundproofing);
        roomType.setHasCoffeeMachine(hasCoffeeMachine);

        return roomType;
    }

    private HotelPricingProfile resolveProfile(Hotel hotel) {
        String name = safe(hotel.getName());
        String country = safe(hotel.getCountry());

        // Luxury / upper-upscale
        if (containsAny(name,
                "omni", "loews", "hotel x toronto", "georgian court",
                "hilton santa clara", "renaissance", "marriott", "embassy suites")) {
            return buildProfile(country,
                    "King Room", "Deluxe King Room", "One-Bedroom Suite",
                    bd("30.00"), bd("38.00"), bd("55.00"),
                    bd("185.00"), bd("255.00"), bd("390.00")
            );
        }

        // Budget / limited service
        if (containsAny(name,
                "one ", "sr hotel", "hotel lotus", "wingate", "stadium hotel",
                "fairfield", "hampton inn", "city express")) {
            return buildProfile(country,
                    "Standard Queen Room", "Superior King Room", "Family Suite",
                    bd("22.00"), bd("28.00"), bd("40.00"),
                    bd("95.00"), bd("135.00"), bd("210.00")
            );
        }

        // Mid-scale default
        return buildProfile(country,
                "Standard King Room", "Deluxe Double Room", "Junior Suite",
                bd("26.00"), bd("33.00"), bd("46.00"),
                bd("130.00"), bd("180.00"), bd("275.00")
        );
    }

    private HotelPricingProfile buildProfile(
            String country,
            String standardName,
            String deluxeName,
            String suiteName,
            BigDecimal standardSize,
            BigDecimal deluxeSize,
            BigDecimal suiteSize,
            BigDecimal standardBase,
            BigDecimal deluxeBase,
            BigDecimal suiteBase
    ) {
        BigDecimal countryMultiplier = countryMultiplier(country);

        return new HotelPricingProfile(
                standardName,
                deluxeName,
                suiteName,
                standardSize,
                deluxeSize,
                suiteSize,
                applyMultiplier(standardBase, countryMultiplier),
                applyMultiplier(deluxeBase, countryMultiplier),
                applyMultiplier(suiteBase, countryMultiplier)
        );
    }

    private BigDecimal countryMultiplier(String country) {
        return switch (country.toLowerCase()) {
            case "united states" -> bd("1.00");
            case "canada" -> bd("0.97");
            case "mexico" -> bd("0.72");
            default -> bd("1.00");
        };
    }

    private BigDecimal applyMultiplier(BigDecimal base, BigDecimal multiplier) {
        return base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String standardDescription(Hotel hotel) {
        return "Comfortable base room at " + hotel.getName()
                + ", suitable for solo travelers or couples visiting the World Cup host city.";
    }

    private String deluxeDescription(Hotel hotel) {
        return "Upgraded room at " + hotel.getName()
                + " with more space and enhanced amenities for longer or premium stays.";
    }

    private String suiteDescription(Hotel hotel) {
        return "Spacious suite at " + hotel.getName()
                + " designed for families or small groups staying near the stadium area.";
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record HotelPricingProfile(
            String standardName,
            String deluxeName,
            String suiteName,
            BigDecimal standardSizeSqm,
            BigDecimal deluxeSizeSqm,
            BigDecimal suiteSizeSqm,
            BigDecimal standardBasePrice,
            BigDecimal deluxeBasePrice,
            BigDecimal suiteBasePrice
    ) {
    }
}