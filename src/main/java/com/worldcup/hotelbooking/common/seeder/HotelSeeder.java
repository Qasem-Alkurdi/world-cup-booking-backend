package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelStatus;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(4)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class HotelSeeder implements CommandLineRunner {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final HotelRepository hotelRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Hotels already exist. Skipping hotel seeder.");
            return;
        }

        List<AppUser> owners = resolveOwners();
        if (owners.isEmpty()) {
            log.warn("No users found. Skipping hotel seeder.");
            return;
        }

        List<HotelSeed> seeds = buildSeeds();
        List<Hotel> hotels = new ArrayList<>();

        for (int i = 0; i < seeds.size(); i++) {
            AppUser owner = owners.get(i % owners.size());
            hotels.add(toHotel(seeds.get(i), owner));
        }

        hotelRepository.saveAll(hotels);
        log.info("Seeded {} hotels around World Cup 2026 host stadiums.", hotels.size());
    }

    private List<AppUser> resolveOwners() {
        List<AppUser> allUsers = appUserRepository.findAll();
        if (allUsers.isEmpty()) {
            return List.of();
        }

        List<AppUser> managers = allUsers.stream()
                .filter(user -> user.getRoles() != null &&
                        (user.getRoles().contains(Role.MANAGER) || user.getRoles().contains(Role.ADMIN)))
                .toList();

        if (!managers.isEmpty()) {
            return managers;
        }

        AppUser fallbackOwner = allUsers.get(0);
        fallbackOwner.addRole(Role.MANAGER);
        appUserRepository.save(fallbackOwner);

        return List.of(fallbackOwner);
    }

    private Hotel toHotel(HotelSeed seed, AppUser owner) {
        Hotel hotel = new Hotel();

        hotel.setOwner(owner);
        hotel.setName(seed.name);
        hotel.setDescription(seed.description);
        hotel.setContactEmail(seed.contactEmail);
        hotel.setContactPhone(seed.contactPhone);

        hotel.setCountry(seed.country);
        hotel.setCity(seed.city);
        hotel.setAddressLine(seed.addressLine);
        hotel.setLocation(point(seed.latitude, seed.longitude));

        hotel.setStatus(HotelStatus.APPROVED);
        hotel.setDeleted(false);

        hotel.setHasWifi(seed.hasWifi);
        hotel.setHasParking(seed.hasParking);
        hotel.setHasBreakfast(seed.hasBreakfast);
        hotel.setHasAirConditioning(seed.hasAirConditioning);
        hotel.setHasHeating(seed.hasHeating);
        hotel.setHasElevator(seed.hasElevator);
        hotel.setHasRestaurant(seed.hasRestaurant);
        hotel.setHasRoomService(seed.hasRoomService);
        hotel.setHasGym(seed.hasGym);
        hotel.setHasPool(seed.hasPool);
        hotel.setHasSpa(seed.hasSpa);
        hotel.setHasLaundry(seed.hasLaundry);
        hotel.setHasAirportShuttle(seed.hasAirportShuttle);
        hotel.setHasAccessibleFacilities(seed.hasAccessibleFacilities);
        hotel.setPetFriendly(seed.petFriendly);
        hotel.setReviewCount(0);
        hotel.setAverageRating(BigDecimal.ZERO);
        return hotel;
    }

    private Point point(double latitude, double longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private List<HotelSeed> buildSeeds() {
        return List.of(
                // ---------------- USA ----------------

                // Mercedes-Benz Stadium — Atlanta
                seed(
                        "Omni Atlanta Hotel at Centennial Park",
                        "Downtown Atlanta hotel near Mercedes-Benz Stadium.",
                        "reservations.atlanta@omnihotels.com",
                        "+1-404-659-0000",
                        "United States", "Atlanta",
                        "190 Marietta St NW, Atlanta, GA 30303",
                        33.7603, -84.3951,
                        true, true, true, true, true, true, true, true, true, false, false, true, false, true, false
                ),
                seed(
                        "Embassy Suites by Hilton Atlanta at Centennial Olympic Park",
                        "All-suite hotel in downtown Atlanta convenient for Mercedes-Benz Stadium.",
                        "ATLCP_Embassy@hilton.com",
                        "+1-404-223-2300",
                        "United States", "Atlanta",
                        "267 Marietta St NW, Atlanta, GA 30313",
                        33.7627, -84.3942,
                        true, true, true, true, true, true, true, false, true, false, false, true, false, true, false
                ),

                // Gillette Stadium — Foxborough/Boston
                seed(
                        "Renaissance Boston Patriot Place Hotel",
                        "Hotel at Patriot Place beside Gillette Stadium.",
                        "info@renaissancepatriotplace.com",
                        "+1-508-543-5500",
                        "United States", "Foxborough",
                        "28 Patriot Place, Foxborough, MA 02035",
                        42.0913, -71.2647,
                        true, true, true, true, true, true, true, true, true, false, false, true, false, true, false
                ),
                seed(
                        "Hilton Garden Inn Foxborough Patriot Place",
                        "Hotel at Patriot Place close to Gillette Stadium.",
                        "BOSFP_GM@hilton.com",
                        "+1-508-543-2040",
                        "United States", "Foxborough",
                        "27 Patriot Place, Foxborough, MA 02035",
                        42.0899, -71.2656,
                        true, true, false, true, true, true, true, true, true, true, false, true, false, true, false
                ),

                // AT&T Stadium — Arlington/Dallas
                seed(
                        "Live! by Loews - Arlington, TX",
                        "Modern hotel in Arlington’s entertainment district near AT&T Stadium.",
                        "reservations@loewshotels.com",
                        "+1-682-277-4900",
                        "United States", "Arlington",
                        "1600 E Randol Mill Rd, Arlington, TX 76011",
                        32.7478, -97.0829,
                        true, true, true, true, true, true, true, true, true, true, true, true, false, true, true
                ),
                seed(
                        "Loews Arlington Hotel",
                        "Large resort-style hotel between Arlington’s major sports venues.",
                        "reservations@loewshotels.com",
                        "+1-682-318-2810",
                        "United States", "Arlington",
                        "888 Nolan Ryan Expy, Arlington, TX 76011",
                        32.7470, -97.0848,
                        true, true, true, true, true, true, true, true, true, true, true, true, false, true, true
                ),

                // NRG Stadium — Houston
                seed(
                        "Holiday Inn Houston S - NRG Area - Med Ctr",
                        "Convenient hotel close to NRG Stadium and NRG Park.",
                        "frontdesk@holidayinnnrg.com",
                        "+1-713-790-1900",
                        "United States", "Houston",
                        "8111 Kirby Dr, Houston, TX 77054",
                        29.6915, -95.4120,
                        true, true, true, true, true, true, true, true, true, true, false, true, false, true, false
                ),
                seed(
                        "Wingate by Wyndham Houston near NRG Park/Medical Center",
                        "Hotel near the NRG / Medical Center district.",
                        "gm@wingatenrghouston.com",
                        "+1-713-797-9400",
                        "United States", "Houston",
                        "8600 Kirby Dr, Houston, TX 77054",
                        29.6858, -95.4123,
                        true, true, true, true, true, true, false, false, true, true, false, true, false, true, false
                ),

                // Arrowhead Stadium — Kansas City
                seed(
                        "Hotel Lotus Kansas City Stadium",
                        "Budget-friendly hotel near Truman Sports Complex and Arrowhead Stadium.",
                        "hotellotusstadium@gmail.com",
                        "+1-816-921-6000",
                        "United States", "Kansas City",
                        "3830 Blue Ridge Cutoff, Kansas City, MO 64133",
                        39.0520, -94.5230,
                        true, true, true, true, true, false, false, false, false, true, false, true, false, true, false
                ),
                seed(
                        "Best Western Premier Kansas City Sports Complex Hotel",
                        "Hotel close to Arrowhead Stadium and Kauffman Stadium.",
                        "info@bwpremierkc.com",
                        "+1-816-353-5300",
                        "United States", "Kansas City",
                        "4011 Blue Ridge Cutoff, Kansas City, MO 64133",
                        39.0512, -94.5190,
                        true, true, true, true, true, true, true, false, true, true, false, true, false, true, false
                ),

                // SoFi Stadium — Los Angeles/Inglewood
                seed(
                        "The Lum Hotel Los Angeles Stadium District",
                        "Stadium-district hotel in Inglewood near SoFi Stadium.",
                        "info@thelumhotel.com",
                        "+1-310-419-1234",
                        "United States", "Inglewood",
                        "3900 W Century Blvd, Inglewood, CA 90303",
                        33.9456, -118.3318,
                        true, true, false, true, true, true, true, false, true, true, false, true, true, true, true
                ),
                seed(
                        "Hollywood Park Marriott",
                        "Hotel adjacent to the Hollywood Park / SoFi Stadium area.",
                        "LAXHC_FrontDesk@marriott.com",
                        "+1-310-641-5700",
                        "United States", "Inglewood",
                        "1400 Parkview Ave, Manhattan Beach, CA 90266",
                        33.9016, -118.3857,
                        true, true, false, true, true, true, true, false, true, true, false, true, false, true, false
                ),

                // Hard Rock Stadium — Miami
                seed(
                        "Stadium Hotel",
                        "Miami Gardens hotel very close to Hard Rock Stadium.",
                        "info@stadiumhotel.com",
                        "+1-305-621-5801",
                        "United States", "Miami Gardens",
                        "21485 NW 27th Ave, Miami Gardens, FL 33056",
                        25.9713, -80.2451,
                        true, true, false, true, true, true, false, false, false, true, false, true, false, true, false
                ),
                seed(
                        "SpringHill Suites by Marriott Fort Lauderdale Miramar",
                        "Hotel in the north Miami / Miramar corridor practical for Hard Rock Stadium stays.",
                        "frontdesk@springhillmiramar.com",
                        "+1-954-441-4242",
                        "United States", "Miramar",
                        "10880 Marks Way, Miramar, FL 33025",
                        25.9794, -80.2928,
                        true, true, true, true, true, true, false, false, true, true, false, true, false, true, false
                ),

                // MetLife Stadium — New York/New Jersey
                seed(
                        "Hampton Inn Carlstadt-At The Meadowlands",
                        "Hotel adjacent to the Meadowlands complex near MetLife Stadium.",
                        "CLDNJ_Hampton@hilton.com",
                        "+1-201-935-9000",
                        "United States", "Carlstadt",
                        "304 Paterson Plank Rd, Carlstadt, NJ 07072",
                        40.8309, -74.0756,
                        true, true, true, true, true, true, false, false, true, false, false, true, false, true, true
                ),
                seed(
                        "Fairfield Inn by Marriott East Rutherford Meadowlands",
                        "Hotel near the Meadowlands sports complex.",
                        "EWRER_GM@marriott.com",
                        "+1-201-507-5222",
                        "United States", "East Rutherford",
                        "850 Paterson Plank Rd, East Rutherford, NJ 07073",
                        40.8188, -74.0807,
                        true, true, true, true, true, true, false, false, true, false, false, true, false, true, false
                ),

                // Lincoln Financial Field — Philadelphia
                seed(
                        "Courtyard by Marriott Philadelphia South at The Navy Yard",
                        "Hotel in South Philadelphia near Lincoln Financial Field.",
                        "phlcs_gm@courtyard.com",
                        "+1-215-644-9200",
                        "United States", "Philadelphia",
                        "1001 Intrepid Ave, Philadelphia, PA 19112",
                        39.8949, -75.1796,
                        true, true, false, true, true, true, true, false, true, false, false, true, false, true, false
                ),
                seed(
                        "Holiday Inn Philadelphia Arpt-Stadium Area",
                        "Airport/stadium-area hotel practical for Lincoln Financial Field.",
                        "frontdesk@hiphilstadium.com",
                        "+1-215-755-9500",
                        "United States", "Philadelphia",
                        "2033 Penrose Ave, Philadelphia, PA 19145",
                        39.9142, -75.1783,
                        true, true, true, true, true, true, true, true, true, true, false, true, true, true, false
                ),

                // Levi's Stadium — Santa Clara / San Francisco Bay Area
                seed(
                        "Hilton Santa Clara",
                        "Hotel near Levi's Stadium and the Santa Clara Convention Center.",
                        "SJCSC_Hotel@hilton.com",
                        "+1-408-330-0001",
                        "United States", "Santa Clara",
                        "4949 Great America Pkwy, Santa Clara, CA 95054",
                        37.4063, -121.9741,
                        true, true, true, true, true, true, true, true, true, true, false, true, false, true, false
                ),
                seed(
                        "Santa Clara Marriott",
                        "Convention-center hotel close to Levi's Stadium.",
                        "SJCGA_FrontDesk@marriott.com",
                        "+1-408-988-1500",
                        "United States", "Santa Clara",
                        "2700 Mission College Blvd, Santa Clara, CA 95054",
                        37.3898, -121.9780,
                        true, true, false, true, true, true, true, true, true, true, false, true, false, true, false
                ),

                // Lumen Field — Seattle
                seed(
                        "Silver Cloud Hotel Seattle - Stadium",
                        "Hotel next to Lumen Field and across from T-Mobile Park.",
                        "stadium@silvercloud.com",
                        "+1-206-204-9800",
                        "United States", "Seattle",
                        "1046 1st Ave S, Seattle, WA 98134",
                        47.5922, -122.3327,
                        true, true, false, true, true, true, true, false, true, true, false, true, false, true, false
                ),
                seed(
                        "Embassy Suites by Hilton Seattle Downtown Pioneer Square",
                        "Hotel in Pioneer Square within easy reach of Lumen Field.",
                        "SEADW_Embassy@hilton.com",
                        "+1-206-447-1444",
                        "United States", "Seattle",
                        "255 S King St, Seattle, WA 98104",
                        47.5983, -122.3293,
                        true, true, true, true, true, true, true, false, true, false, false, true, false, true, false
                ),

                // ---------------- CANADA ----------------

                // BMO Field — Toronto
                seed(
                        "Hotel X Toronto",
                        "Lakefront hotel on Exhibition Place grounds near BMO Field.",
                        "connect@hotelxtoronto.com",
                        "+1-855-943-9300",
                        "Canada", "Toronto",
                        "111 Princes' Blvd, Toronto, ON M6K 3C3",
                        43.6347, -79.4195,
                        true, true, true, true, true, true, true, true, true, true, true, true, true, true, true
                ),
                seed(
                        "Gladstone House",
                        "West-end Toronto boutique hotel practical for Exhibition Place and BMO Field.",
                        "stay@gladstonehouse.ca",
                        "+1-416-531-4635",
                        "Canada", "Toronto",
                        "1214 Queen St W, Toronto, ON M6J 1J6",
                        43.6426, -79.4268,
                        true, false, false, true, true, true, true, true, false, false, false, true, false, true, true
                ),

                // BC Place — Vancouver
                seed(
                        "Georgian Court Hotel",
                        "Downtown Vancouver hotel steps from BC Place Stadium.",
                        "frontdesk@georgiancourt.com",
                        "+1-604-682-5555",
                        "Canada", "Vancouver",
                        "773 Beatty St, Vancouver, BC V6B 2M4",
                        49.2776, -123.1115,
                        true, true, false, true, true, true, true, true, true, false, true, true, false, true, true
                ),
                seed(
                        "YWCA Hotel Vancouver",
                        "Affordable downtown Vancouver hotel across from BC Place.",
                        "reservations@ywcavan.org",
                        "+1-604-895-5830",
                        "Canada", "Vancouver",
                        "733 Beatty St, Vancouver, BC V6B 2M4",
                        49.2771, -123.1110,
                        true, false, false, true, true, true, false, false, false, false, false, true, false, true, false
                ),

                // ---------------- MEXICO ----------------

                // Estadio Akron — Guadalajara / Zapopan
                seed(
                        "one Guadalajara Periférico Poniente",
                        "Hotel a few minutes from Estadio Akron.",
                        "reservaciones@onehoteles.com",
                        "+52-33-1930-9572",
                        "Mexico", "Zapopan",
                        "Periférico Poniente 7306, Fracc. Anillo Periférico, Zapopan, Jalisco 45010",
                        20.6735, -103.4496,
                        true, true, true, true, false, true, false, false, false, false, false, true, false, true, false
                ),
                seed(
                        "SR Hotel",
                        "Zapopan hotel practical for the Akron / Ciudad Judicial area.",
                        "reservaciones@srhotel.com.mx",
                        "+52-33-3110-1974",
                        "Mexico", "Zapopan",
                        "Periférico Poniente 7300, Zapopan, Jalisco 45010",
                        20.6731, -103.4508,
                        true, true, false, true, false, true, false, true, false, false, false, true, false, true, false
                ),

                // Estadio Azteca — Mexico City
                seed(
                        "City Express Plus by Marriott Ciudad de México Periférico Sur Tlalpan",
                        "South Mexico City hotel suitable for Estadio Azteca area stays.",
                        "reservaciones@cityexpress.com",
                        "+52-55-5483-6700",
                        "Mexico", "Ciudad de Mexico",
                        "Blvd. Adolfo Ruiz Cortines 4860, Col. Guadalupe, Ciudad de Mexico 14388",
                        19.2995, -99.2140,
                        true, true, true, true, false, true, true, true, true, false, false, true, false, true, false
                ),
                seed(
                        "Radisson Paraiso Hotel Mexico City",
                        "Large south-city hotel practical for Estadio Azteca access.",
                        "reservations.mexico@radisson.com",
                        "+52-55-5927-5959",
                        "Mexico", "Ciudad de Mexico",
                        "Cuspide 53, Parques del Pedregal, Tlalpan, Ciudad de Mexico 14010",
                        19.3047, -99.2138,
                        true, true, true, true, false, true, true, true, true, false, false, true, false, true, false
                ),

                // Estadio BBVA — Monterrey / Guadalupe / Apodaca
                seed(
                        "Holiday Inn & Suites Monterrey Apodaca Zona Airport",
                        "Monterrey-area hotel useful for airport and eastern metro access toward Estadio BBVA.",
                        "reservaciones@himtyapodaca.com",
                        "+52-81-2477-0800",
                        "Mexico", "Apodaca",
                        "Carretera Miguel Aleman 120, Parque Industrial Milimex, Apodaca, Nuevo Leon 66601",
                        25.7780, -100.1820,
                        true, true, true, true, true, true, true, true, true, true, false, true, true, true, false
                ),
                seed(
                        "CHN Hotel Monterrey Aeropuerto, Trademark by Wyndham",
                        "Eastern Monterrey metro hotel that can serve Estadio BBVA visitors.",
                        "reservaciones@chnhotel.com",
                        "+52-81-1454-2800",
                        "Mexico", "Apodaca",
                        "Ave. Miguel Aleman 533, Apodaca, Nuevo Leon 66600",
                        25.7586, -100.2153,
                        true, true, true, true, true, true, false, false, true, true, false, true, true, true, false
                )
        );
    }

    private HotelSeed seed(
            String name,
            String description,
            String contactEmail,
            String contactPhone,
            String country,
            String city,
            String addressLine,
            double latitude,
            double longitude,
            boolean hasWifi,
            boolean hasParking,
            boolean hasBreakfast,
            boolean hasAirConditioning,
            boolean hasHeating,
            boolean hasElevator,
            boolean hasRestaurant,
            boolean hasRoomService,
            boolean hasGym,
            boolean hasPool,
            boolean hasSpa,
            boolean hasLaundry,
            boolean hasAirportShuttle,
            boolean hasAccessibleFacilities,
            boolean petFriendly
    ) {
        return new HotelSeed(
                name, description, contactEmail, contactPhone,
                country, city, addressLine, latitude, longitude,
                hasWifi, hasParking, hasBreakfast, hasAirConditioning, hasHeating,
                hasElevator, hasRestaurant, hasRoomService, hasGym, hasPool, hasSpa,
                hasLaundry, hasAirportShuttle, hasAccessibleFacilities, petFriendly
        );
    }

    private record HotelSeed(String name, String description, String contactEmail, String contactPhone, String country,
                             String city, String addressLine, double latitude, double longitude, boolean hasWifi,
                             boolean hasParking, boolean hasBreakfast, boolean hasAirConditioning, boolean hasHeating,
                             boolean hasElevator, boolean hasRestaurant, boolean hasRoomService, boolean hasGym,
                             boolean hasPool, boolean hasSpa, boolean hasLaundry, boolean hasAirportShuttle,
                             boolean hasAccessibleFacilities, boolean petFriendly) {
    }
}