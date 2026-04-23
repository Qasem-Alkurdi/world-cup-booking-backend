package com.worldcup.hotelbooking.availability_pricing.pricing;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnhancedPricingServiceImplTest {

    @Mock
    private PricingServiceImpl basePricingService;
    @Mock
    private MatchRepository matchRepository;
    @InjectMocks
    private EnhancedPricingServiceImpl enhancedPricingService;

    @Mock
    private Hotel hotel;
    @Mock
    private RoomType roomType;

    // ─── shared match/stadium setup ───────────────────────────────────────────

    private Match buildMatch(Match.MatchStage stage, LocalDate date, String city,
                             boolean opening, boolean derby) {
        Stadium s = mock(Stadium.class);
        when(s.getCity()).thenReturn(city);

        Match m = mock(Match.class);
        when(m.getStage()).thenReturn(stage);
        when(m.getMatchDateTime()).thenReturn(date.atTime(20, 0));
        when(m.getStadium()).thenReturn(s);
        when(m.isOpeningMatch()).thenReturn(opening);
        when(m.isDerby()).thenReturn(derby);
        when(m.getHomeTeam()).thenReturn("Team A");
        when(m.getAwayTeam()).thenReturn("Team B");
        return m;
    }

    @BeforeEach
    void commonStubs() {
        when(hotel.getName()).thenReturn("Test Hotel");
        when(hotel.getCity()).thenReturn("Doha");
        when(hotel.getLatitude()).thenReturn(25.0);
        when(hotel.getLongitude()).thenReturn(51.0);
        when(roomType.getBasePrice()).thenReturn(BigDecimal.valueOf(100));
    }

    // =========================================================================
    // calculateTotalStayPrice(Booking, ...) — delegates to date overload
    // =========================================================================

    @Test
    void calculateTotalStayPrice_booking_noMatches_returnsBasePriceTimesNightsTimesRooms() {
        // 5 nights, 2 rooms, no matches → 100 * 5 * 2 = 1000
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 6));

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(booking, hotel, roomType, 2);

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result));
        verify(matchRepository).findMatchesBetweenDates(any(), any());
        // basePricingService must NOT be called — no matches means base price path
        verifyNoInteractions(basePricingService);
    }

    @Test
    void calculateTotalStayPrice_booking_withMatchOnOneNight_callsBasePricingForThatNight() {
        // 3 nights (June 10-12).  Match on June 11 only.
        // Night June 10 → base price $100
        // Night June 11 → basePricingService returns $200
        // Night June 12 → base price $100 (no match)
        // Total for 1 room = 100 + 200 + 100 = 400
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 6, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 6, 13));

        Match match = buildMatch(Match.MatchStage.GROUP_STAGE_1,
                LocalDate.of(2026, 6, 11), "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of(match));
        when(basePricingService.calculateDynamicPrice(
                any(), any(), any(), any(), anyInt()
        )).thenReturn(BigDecimal.valueOf(200));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(booking, hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(400).compareTo(result));
        // basePricingService called exactly once (only the night with a match)
        verify(basePricingService, times(1)).calculateDynamicPrice(any(), any(), any(), any(), anyInt());
    }

    @Test
    void calculateTotalStayPrice_booking_multipleRooms_multipliesCorrectly() {
        // 2 nights, no matches, 3 rooms → 100 * 2 * 3 = 600
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 3));

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(booking, hotel, roomType, 3);

        assertEquals(0, BigDecimal.valueOf(600).compareTo(result));
    }

    // =========================================================================
    // calculateTotalStayPrice(LocalDate, LocalDate, ...) — overloaded form
    // =========================================================================

    @Test
    void calculateTotalStayPrice_dates_noMatches_returnsBasePriceTimesNightsTimesRooms() {
        // 3 nights, 3 rooms → 80 * 3 * 3 = 720
        when(roomType.getBasePrice()).thenReturn(BigDecimal.valueOf(80));
        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4),
                hotel, roomType, 3);

        assertEquals(0, BigDecimal.valueOf(720).compareTo(result));
        verify(matchRepository).findMatchesBetweenDates(any(), any());
        verifyNoInteractions(basePricingService);
    }

    @Test
    void calculateTotalStayPrice_dates_withMatch_callsBasePricing() {
        // 1 night (June 15), match on June 15 → basePricing returns $300
        Match match = buildMatch(Match.MatchStage.QUARTER_FINAL,
                LocalDate.of(2026, 6, 15), "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of(match));
        when(basePricingService.calculateDynamicPrice(any(), any(), any(), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(300));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 16),
                hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(300).compareTo(result));
        verify(basePricingService, times(1))
                .calculateDynamicPrice(any(), any(), any(), any(), anyInt());
    }

    @Test
    void calculateTotalStayPrice_dates_checkoutBeforeCheckin_throwsIllegalArgument() {
        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                enhancedPricingService.calculateTotalStayPrice(
                        LocalDate.of(2026, 7, 10),
                        LocalDate.of(2026, 7, 5),  // before check-in
                        hotel, roomType, 1));
    }

    @Test
    void calculateTotalStayPrice_dates_sameDayCheckInOut_throwsIllegalArgument() {
        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        LocalDate same = LocalDate.of(2026, 7, 10);
        assertThrows(IllegalArgumentException.class, () ->
                enhancedPricingService.calculateTotalStayPrice(same, same, hotel, roomType, 1));
    }

    // =========================================================================
    // Night-by-night pricing logic
    // =========================================================================

    @Test
    void calculateTotalStayPrice_eachNightPricedIndependently_higherStageNightIsMostExpensive() {
        // Stay: June 10 → June 13  (3 nights: 10, 11, 12)
        // Match on June 10: GROUP_STAGE_1  → basePricing returns $150
        // Match on June 11: FINAL          → basePricing returns $500
        // June 12: no match               → base price $100
        // Total for 1 room = 150 + 500 + 100 = 750

        Match groupMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1,
                LocalDate.of(2026, 6, 10), "Doha", false, false);
        Match finalMatch = buildMatch(Match.MatchStage.FINAL,
                LocalDate.of(2026, 6, 11), "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any()))
                .thenReturn(List.of(groupMatch, finalMatch));

        // Return different prices depending on the match stage passed in
        when(basePricingService.calculateDynamicPrice(
                any(), any(), eq(groupMatch), any(), anyInt())).thenReturn(BigDecimal.valueOf(150));
        when(basePricingService.calculateDynamicPrice(
                any(), any(), eq(finalMatch), any(), anyInt())).thenReturn(BigDecimal.valueOf(500));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 13),
                hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(750).compareTo(result));
        // basePricingService called once for each match night (2 nights with matches)
        verify(basePricingService, times(2))
                .calculateDynamicPrice(any(), any(), any(), any(), anyInt());
    }

    @Test
    void calculateTotalStayPrice_multipleMatchesSameNight_picksHighestImportance() {
        // Two matches on the same night — FINAL should beat GROUP_STAGE_1
        LocalDate night = LocalDate.of(2026, 6, 15);

        Match lowMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1, night, "Cairo", false, false);
        Match highMatch = buildMatch(Match.MatchStage.FINAL, night, "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any()))
                .thenReturn(List.of(lowMatch, highMatch));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(highMatch), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(600));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(lowMatch), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(100));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                night, night.plusDays(1), hotel, roomType, 1);

        // Should use the FINAL match price ($600), not the group stage ($100)
        assertEquals(0, BigDecimal.valueOf(600).compareTo(result));
        verify(basePricingService).calculateDynamicPrice(any(), any(), eq(highMatch), any(), anyInt());
        verify(basePricingService, never()).calculateDynamicPrice(any(), any(), eq(lowMatch), any(), anyInt());
    }

    @Test
    void calculateTotalStayPrice_prefersMatchInSameCityAsHotel_whenImportanceIsEqual() {
        // Two GROUP_STAGE_1 matches on same night: one in hotel city (Doha), one in Cairo
        // When importance is equal, same-city match should be preferred
        LocalDate night = LocalDate.of(2026, 6, 11);

        Match dohaMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1, night, "Doha", false, false);
        Match cairoMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1, night, "Cairo", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any()))
                .thenReturn(List.of(cairoMatch, dohaMatch));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(dohaMatch), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(180));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(cairoMatch), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                night, night.plusDays(1), hotel, roomType, 1);

        // Doha match should win the tiebreak → $180
        assertEquals(0, BigDecimal.valueOf(180).compareTo(result));
    }

    @Test
    void calculateTotalStayPrice_openingMatchBonus_increasesImportanceScore() {
        // Opening match should beat a normal FINAL-stage match when both are on same night
        // (opening match adds +50 to score; FINAL = 100, GROUP_STAGE_1+opening = 10+50 = 60)
        // Actually FINAL (100) > GROUP_STAGE_1+opening (60), so FINAL still wins.
        // This test verifies opening match beats a lower stage (ROUND_OF_16 = 40 vs GROUP+opening = 60)
        LocalDate night = LocalDate.of(2026, 6, 11);

        Match openingMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1, night, "Cairo", true, false);
        Match r16Match = buildMatch(Match.MatchStage.ROUND_OF_16, night, "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any()))
                .thenReturn(List.of(r16Match, openingMatch));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(openingMatch), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(400));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(r16Match), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(250));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                night, night.plusDays(1), hotel, roomType, 1);

        // Opening match score = 10 + 50 = 60 > R16 score = 40 → opening match wins
        assertEquals(0, BigDecimal.valueOf(400).compareTo(result));
        verify(basePricingService).calculateDynamicPrice(any(), any(), eq(openingMatch), any(), anyInt());
        verify(basePricingService, never()).calculateDynamicPrice(any(), any(), eq(r16Match), any(), anyInt());
    }

    // =========================================================================
    // getMultiNightBreakdown
    // =========================================================================

    @Test
    void getMultiNightBreakdown_noMatches_returnsSinglePhaseWithBasePricing() {
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 4)); // 3 nights

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                enhancedPricingService.getMultiNightBreakdown(booking, hotel, roomType, 1);

        assertNotNull(breakdown);
        // All 3 nights share the same label ("No Match Night") and same rate ($100)
        // so they collapse into a single PhaseBreakdown entry
        assertEquals(1, breakdown.getPhases().size());
        assertEquals(0, BigDecimal.valueOf(300).compareTo(breakdown.getTotalPrice()));
        assertEquals("No Match Night", breakdown.getPhases().get(0).getPhaseName());
        assertEquals(3, breakdown.getPhases().get(0).getNights());
    }

    @Test
    void getMultiNightBreakdown_withMixedNights_groupsConsecutiveSameLabelNights() {
        // 4 nights: June 10, 11, 12, 13
        // Match on June 11 and 12 (GROUP_STAGE_1) → 2 "Group Stage" nights
        // June 10 and 13 → "No Match Night"
        // Expect 3 PhaseBreakdown entries: [NoMatch(1), GroupStage(2), NoMatch(1)]
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 6, 10));
        booking.setCheckOutDate(LocalDate.of(2026, 6, 14));

        Match match11 = buildMatch(Match.MatchStage.GROUP_STAGE_1, LocalDate.of(2026, 6, 11), "Doha", false, false);
        Match match12 = buildMatch(Match.MatchStage.GROUP_STAGE_1, LocalDate.of(2026, 6, 12), "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any()))
                .thenReturn(List.of(match11, match12));
        when(basePricingService.calculateDynamicPrice(any(), any(), any(), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(150));

        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                enhancedPricingService.getMultiNightBreakdown(booking, hotel, roomType, 1);

        assertNotNull(breakdown);
        assertEquals(3, breakdown.getPhases().size());

        // First entry: 1 no-match night (June 10)
        assertEquals("No Match Night", breakdown.getPhases().get(0).getPhaseName());
        assertEquals(1, breakdown.getPhases().get(0).getNights());

        // Second entry: 2 group-stage nights (June 11-12)
        assertEquals("Group Stage (Matchday 1) Night", breakdown.getPhases().get(1).getPhaseName());
        assertEquals(2, breakdown.getPhases().get(1).getNights());

        // Third entry: 1 no-match night (June 13)
        assertEquals("No Match Night", breakdown.getPhases().get(2).getPhaseName());
        assertEquals(1, breakdown.getPhases().get(2).getNights());

        // Total = 100 + 150 + 150 + 100 = 500
        assertEquals(0, BigDecimal.valueOf(500).compareTo(breakdown.getTotalPrice()));
    }

    @Test
    void getMultiNightBreakdown_averageRateIsCorrect() {
        // 2 nights, no matches, base price $100 → total $200, avg $100/night
        Booking booking = new Booking();
        booking.setCheckInDate(LocalDate.of(2026, 7, 5));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 7));

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of());

        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                enhancedPricingService.getMultiNightBreakdown(booking, hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(100).compareTo(breakdown.getAverageNightlyRate()));
    }

    // =========================================================================
    // MultiNightPricingBreakdown inner class
    // =========================================================================

    @Test
    void breakdownExplanation_emptyPhases_returnsStandardPricingMessage() {
        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                new EnhancedPricingServiceImpl.MultiNightPricingBreakdown(
                        List.of(), BigDecimal.valueOf(500), BigDecimal.valueOf(100));

        String explanation = breakdown.getExplanation();

        assertTrue(explanation.contains("Standard"));
    }

    @Test
    void breakdownExplanation_withPhases_containsPhaseNameAndAmounts() {
        EnhancedPricingServiceImpl.PhaseBreakdown phase =
                new EnhancedPricingServiceImpl.PhaseBreakdown(
                        "Final Night",
                        LocalDate.of(2026, 7, 19),
                        LocalDate.of(2026, 7, 20),
                        1,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(500));

        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                new EnhancedPricingServiceImpl.MultiNightPricingBreakdown(
                        List.of(phase), BigDecimal.valueOf(500), BigDecimal.valueOf(500));

        String explanation = breakdown.getExplanation();

        assertTrue(explanation.contains("Final Night"));
        assertTrue(explanation.contains("500"));
        assertTrue(explanation.contains("Total"));
    }

    @Test
    void breakdownExplanation_emptyPhases_doesNotContainPerNightBreakdown() {
        EnhancedPricingServiceImpl.MultiNightPricingBreakdown breakdown =
                new EnhancedPricingServiceImpl.MultiNightPricingBreakdown(
                        List.of(), BigDecimal.ZERO, BigDecimal.ZERO);

        assertFalse(breakdown.getExplanation().contains("night(s)"));
    }

    // =========================================================================
    // Occupancy estimation — verify correct stage → occupancy mapping
    // =========================================================================

    @Test
    void finalNight_usesHighestOccupancyEstimate() {
        // FINAL stage → estimateOccupancy = 98 → high demand multiplier
        // We verify basePricingService is called with the FINAL match
        LocalDate night = LocalDate.of(2026, 7, 19);
        Match finalMatch = buildMatch(Match.MatchStage.FINAL, night, "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of(finalMatch));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(finalMatch), any(), eq(98)))
                .thenReturn(BigDecimal.valueOf(999));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                night, night.plusDays(1), hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(999).compareTo(result));
        // Verify occupancy 98 was passed for a FINAL match
        verify(basePricingService).calculateDynamicPrice(any(), any(), eq(finalMatch), any(), eq(98));
    }

    @Test
    void groupStage1Night_usesLowestOccupancyEstimate() {
        LocalDate night = LocalDate.of(2026, 6, 12);
        Match groupMatch = buildMatch(Match.MatchStage.GROUP_STAGE_1, night, "Doha", false, false);

        when(matchRepository.findMatchesBetweenDates(any(), any())).thenReturn(List.of(groupMatch));
        when(basePricingService.calculateDynamicPrice(any(), any(), eq(groupMatch), any(), eq(55)))
                .thenReturn(BigDecimal.valueOf(150));

        BigDecimal result = enhancedPricingService.calculateTotalStayPrice(
                night, night.plusDays(1), hotel, roomType, 1);

        assertEquals(0, BigDecimal.valueOf(150).compareTo(result));
        // Verify occupancy 55 was passed for a GROUP_STAGE_1 match
        verify(basePricingService).calculateDynamicPrice(any(), any(), eq(groupMatch), any(), eq(55));
    }
}