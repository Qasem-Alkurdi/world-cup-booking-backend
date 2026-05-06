package com.worldcup.hotelbooking.availability_pricing.pricing;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private PricingConfig pricingConfig;
    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private PricingConfig.DistanceMultipliers distanceMultipliers;
    @Mock
    private PricingConfig.MatchMultipliers matchMultipliers;
    @Mock
    private PricingConfig.DemandMultipliers demandMultipliers;
    @Mock
    private PricingConfig.TimeMultipliers timeMultipliers;

    private PricingServiceImpl pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl(pricingConfig, hotelRepository);

        // Mock the distance calculation for different test scenarios
        when(hotelRepository.calculateDistanceInMeters(1L, 1L)).thenReturn(1000.0); // 1 km (walking distance)
        when(hotelRepository.calculateDistanceInMeters(1L, 2L)).thenReturn(6670.0); // ~6.67 km (medium distance)
        when(hotelRepository.calculateDistanceInMeters(1L, 3L)).thenReturn(13340.0); // ~13.34 km (far distance)

        when(pricingConfig.getDistance()).thenReturn(distanceMultipliers);
        when(pricingConfig.getMatch()).thenReturn(matchMultipliers);
        when(pricingConfig.getDemand()).thenReturn(demandMultipliers);
        when(pricingConfig.getTime()).thenReturn(timeMultipliers);

        stubDistanceMultipliers();
        stubMatchMultipliers();
        stubDemandMultipliers();
        stubTimeMultipliers();
    }

    @Test
    void calculateDynamicPrice_shouldReturnCorrectPrice_withPopularTeamBonusDerbyAndWalkingDistance() {
        // Arrange
        RoomType roomType = createRoomType(BigDecimal.valueOf(100), "Deluxe");
        Hotel hotel = createHotel(30.0000, 31.0000, 1L);
        Match match = createMatch(
                "Brazil",
                "Morocco",
                Match.MatchStage.FINAL,
                LocalDateTime.of(2026, 6, 28, 20, 0), // Friday
                true,
                true,
                createStadium(30.0100, 31.0000, 1L)
        );
        LocalDate bookingDate = LocalDate.of(2026, 6, 23);
        int occupancy = 95;

        // Expected multipliers (ADAPTED TO ADDITION LOGIC):
        // distance <= 2km => 2.5
        // opening match => 2.8
        // occupancy 95 => 1.8, days until match 5 => 1.5, average => 1.65
        // Friday => 1.3
        // final price = 100 * (2.5 + 2.8 + 1.65 + 1.3) = 100 * 8.25 = 825.00

        // Act
        BigDecimal result = pricingService.calculateDynamicPrice(roomType, hotel, match, bookingDate, occupancy);

        // Assert
        assertEquals(0, BigDecimal.valueOf(825.00).compareTo(result));
    }

    @Test
    void calculateDynamicPrice_shouldApplyPopularTeamAndDerbyBonuses_whenNotOpeningMatch() {
        // Arrange
        RoomType roomType = createRoomType(BigDecimal.valueOf(100), "Standard");
        Hotel hotel = createHotel(30.0000, 31.0000, 1L);
        Match match = createMatch(
                "Brazil",
                "Argentina",
                Match.MatchStage.SEMI_FINAL,
                LocalDateTime.of(2026, 7, 8, 20, 0), // Wednesday
                false,
                true,
                createStadium(30.0200, 31.0000, 2L)
        );
        LocalDate bookingDate = LocalDate.of(2026, 6, 22);
        int occupancy = 80;

        // Expected multipliers (ADAPTED TO ADDITION LOGIC):
        // distance 6.67km => medium 1.5
        // semi-final 3.0 + popular 0.3 + derby 0.5 = 3.8
        // occupancy 80 => 1.5, days until match 17 => 1.0, average => 1.25
        // Wednesday => 1.0
        // final price = 100 * (1.5 + 3.8 + 1.25 + 1.0) = 100 * 7.55 = 755.00

        // Act
        BigDecimal result = pricingService.calculateDynamicPrice(roomType, hotel, match, bookingDate, occupancy);

        // Assert
        assertEquals(0, BigDecimal.valueOf(755.00).compareTo(result));
    }

    @Test
    void calculateDynamicPrice_shouldUseMediumDistanceStandardDemandAndWeekdayMultiplier() {
        // Arrange
        RoomType roomType = createRoomType(BigDecimal.valueOf(200), "Suite");
        Hotel hotel = createHotel(30.0000, 31.0000, 1L);
        Match match = createMatch(
                "Japan",
                "Mexico",
                Match.MatchStage.GROUP_STAGE_2,
                LocalDateTime.of(2026, 7, 1, 18, 0), // Wednesday
                false,
                false,
                createStadium(30.0600, 31.0000, 2L)
        );
        LocalDate bookingDate = LocalDate.of(2026, 6, 11);
        int occupancy = 45;

        // Expected multipliers (ADAPTED TO ADDITION LOGIC):
        // distance 6.67km => medium 1.5
        // group stage 2 => 1.4 (no bonuses as not opening, not derby, not popular team combo)
        // occupancy 45 => 1.1, days until match 20 => 1.0, average => 1.05
        // Wednesday => 1.0
        // final price = 200 * (1.5 + 1.4 + 1.05 + 1.0) = 200 * 4.95 = 990.00

        // Act
        BigDecimal result = pricingService.calculateDynamicPrice(roomType, hotel, match, bookingDate, occupancy);

        // Assert
        assertEquals(0, BigDecimal.valueOf(990.00).compareTo(result));
    }

    @Test
    void getPricingBreakdown_shouldReturnCorrectBreakdownValues() {
        // Arrange
        RoomType roomType = createRoomType(BigDecimal.valueOf(150), "Premium");
        Hotel hotel = createHotel(30.0000, 31.0000, 1L);
        Match match = createMatch(
                "Brazil",
                "Germany",
                Match.MatchStage.QUARTER_FINAL,
                LocalDateTime.of(2026, 7, 4, 21, 0), // Saturday
                false,
                true,
                createStadium(30.1200, 31.0000, 3L)
        );
        LocalDate bookingDate = LocalDate.of(2026, 6, 26);
        int occupancy = 65;

        // Expected multipliers (ADAPTED TO ADDITION LOGIC):
        // distance around 13.34km => far 1.2
        // quarter final 2.5 + popular 0.3 + derby 0.5 = 3.3
        // occupancy 65 => 1.3, days until match 8 => 1.3, average => 1.3
        // Saturday => 1.3
        // final price = 150 * (1.2 + 3.3 + 1.3 + 1.3) = 150 * 7.1 = 1065.00

        // Act
        PricingResponseDto result = pricingService.getPricingBreakdown(roomType, hotel, match, bookingDate, occupancy);

        // Assert
        assertEquals(0, BigDecimal.valueOf(150).compareTo(result.getBasePrice()));
        assertEquals(1.2, result.getDistanceMultiplier());
        assertEquals(3.3, result.getMatchMultiplier(), 0.0001);
        assertEquals(1.3, result.getDemandMultiplier(), 0.0001);
        assertEquals(1.3, result.getTimeMultiplier(), 0.0001);
        assertEquals(0, BigDecimal.valueOf(1065.00).compareTo(result.getFinalPrice()));
        assertEquals(13.34, result.getDistanceFromStadium(), 0.2);
    }

    private void stubDistanceMultipliers() {
        when(distanceMultipliers.getWalkingDistance()).thenReturn(2.5);
        when(distanceMultipliers.getShortDrive()).thenReturn(2.0);
        when(distanceMultipliers.getMedium()).thenReturn(1.5);
        when(distanceMultipliers.getFar()).thenReturn(1.2);
        when(distanceMultipliers.getVeryFar()).thenReturn(1.0);
    }

    private void stubMatchMultipliers() {
        when(matchMultipliers.getFinalMatch()).thenReturn(3.5);
        when(matchMultipliers.getSemiFinal()).thenReturn(3.0);
        when(matchMultipliers.getQuarterFinal()).thenReturn(2.5);
        when(matchMultipliers.getRoundOf16()).thenReturn(2.0);
        when(matchMultipliers.getGroupStage3()).thenReturn(1.8);
        when(matchMultipliers.getGroupStage2()).thenReturn(1.4);
        when(matchMultipliers.getGroupStage1()).thenReturn(1.2);
        when(matchMultipliers.getOpeningMatch()).thenReturn(2.8);
        when(matchMultipliers.getPopularTeamBonus()).thenReturn(0.3);
        when(matchMultipliers.getDerbyBonus()).thenReturn(0.5);
    }

    private void stubDemandMultipliers() {
        when(demandMultipliers.getVeryHighOccupancy()).thenReturn(1.8);
        when(demandMultipliers.getHighOccupancy()).thenReturn(1.5);
        when(demandMultipliers.getMediumHighOccupancy()).thenReturn(1.3);
        when(demandMultipliers.getNormalOccupancy()).thenReturn(1.1);
        when(demandMultipliers.getLowOccupancy()).thenReturn(1.0);
        when(demandMultipliers.getVeryLowOccupancy()).thenReturn(0.9);
        when(demandMultipliers.getLastMinute()).thenReturn(1.5);
        when(demandMultipliers.getLateBooking()).thenReturn(1.3);
        when(demandMultipliers.getStandard()).thenReturn(1.0);
        when(demandMultipliers.getEarlyBird()).thenReturn(0.95);
        when(demandMultipliers.getAdvanceBooking()).thenReturn(0.90);
        when(demandMultipliers.getSuperEarlyBird()).thenReturn(0.85);
    }

    private void stubTimeMultipliers() {
        when(timeMultipliers.getWeekend()).thenReturn(1.3);
        when(timeMultipliers.getPreMatch()).thenReturn(1.2);
        when(timeMultipliers.getWeekday()).thenReturn(1.0);
    }

    private RoomType createRoomType(BigDecimal basePrice, String name) {
        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setBasePrice(basePrice);
        roomType.setCurrency("USD");
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(2);
        roomType.setTotalRooms(10);
        return roomType;
    }

    private Hotel createHotel(double latitude, double longitude, Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setLatitude(latitude);
        hotel.setLongitude(longitude);
        return hotel;
    }

    private Stadium createStadium(double latitude, double longitude, Long id) {
        Stadium stadium = new Stadium();
        stadium.setId(id);
        stadium.setName("Test Stadium");
        stadium.setLatitude(latitude);
        stadium.setLongitude(longitude);
        return stadium;
    }

    private Match createMatch(
            String homeTeam,
            String awayTeam,
            Match.MatchStage stage,
            LocalDateTime matchDateTime,
            boolean openingMatch,
            boolean derby,
            Stadium stadium) {
        Match match = new Match();
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setStage(stage);
        match.setMatchDateTime(matchDateTime);
        match.setOpeningMatch(openingMatch);
        match.setDerby(derby);
        match.setStadium(stadium);
        return match;
    }
}

