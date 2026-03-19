package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.availability_pricing.stadium.Stadium;
import com.worldcup.hotelbooking.availability_pricing.stadium.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class StadiumSeeder implements CommandLineRunner {

    private final StadiumRepository stadiumRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (stadiumRepository.count() > 0) {
            log.info("Stadiums already exist. Skipping seeder.");
            return;
        }

        List<Stadium> stadiums = Arrays.asList(
                // USA
                createStadium("Mercedes-Benz Stadium", "Atlanta", 33.7550, -84.4000, 71000),
                createStadium("Gillette Stadium", "Boston", 42.0909, -71.2643, 65878),
                createStadium("AT&T Stadium", "Dallas", 32.7473, -97.0945, 80000),
                createStadium("NRG Stadium", "Houston", 29.6847, -95.4107, 72220),
                createStadium("Arrowhead Stadium", "Kansas City", 39.0489, -94.4839, 76416),
                createStadium("SoFi Stadium", "Los Angeles", 33.9535, -118.3394, 70240),
                createStadium("Hard Rock Stadium", "Miami", 25.9580, -80.2389, 64767),
                createStadium("MetLife Stadium", "New York/New Jersey", 40.8135, -74.0745, 82500),
                createStadium("Lincoln Financial Field", "Philadelphia", 39.9008, -75.1675, 69596),
                createStadium("Levi's Stadium", "San Francisco Bay Area", 37.4033, -121.9698, 68500),
                createStadium("Lumen Field", "Seattle", 47.5952, -122.3316, 68740),
                // Canada
                createStadium("BMO Field", "Toronto", 43.6333, -79.4186, 30000),
                createStadium("BC Place", "Vancouver", 49.2766, -123.1120, 54500),
                // Mexico
                createStadium("Estadio Akron", "Guadalajara", 20.6818, -103.4628, 46355),
                createStadium("Estadio Azteca", "Mexico City", 19.3030, -99.1505, 87523),
                createStadium("Estadio BBVA", "Monterrey", 25.6697, -100.2443, 53500)
        );

        stadiumRepository.saveAll(stadiums);
        log.info("Seeded {} stadiums for World Cup 2026.", stadiums.size());
    }

    private Stadium createStadium(String name, String city, double lat, double lng, int capacity) {
        Stadium stadium = new Stadium();
        stadium.setName(name);
        stadium.setCity(city);
        stadium.setLatitude(lat);
        stadium.setLongitude(lng);
        stadium.setCapacity(capacity);
        return stadium;
    }
}