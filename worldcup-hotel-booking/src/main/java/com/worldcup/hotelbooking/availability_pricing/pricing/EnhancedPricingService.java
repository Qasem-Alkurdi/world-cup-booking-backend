package com.worldcup.hotelbooking.availability_pricing.pricing;

import com.worldcup.hotelbooking.availability_pricing.match.Match;
import com.worldcup.hotelbooking.availability_pricing.match.MatchRepository;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
//import com.worldcup.hotelbooking.catalog.match.Match;
//import com.worldcup.hotelbooking.catalog.match.MatchRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enhanced pricing service that prevents gaming the system with long stays
 *
 * Problem: User books during Group Stage but stays through Finals
 * Solution: Calculate price for each tournament phase separately
 */
@Service
public class EnhancedPricingService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedPricingService.class);

    private final PricingService basePricingService;
    private final MatchRepository matchRepository;

    public EnhancedPricingService(
            PricingService basePricingService,
            MatchRepository matchRepository) {
        this.basePricingService = basePricingService;
        this.matchRepository = matchRepository;
    }

    /**
     * Calculate total price for entire stay considering ALL matches during the period
     * This prevents users from exploiting cheap dates to cover expensive periods
     */
    public BigDecimal calculateTotalStayPrice(
            Booking booking,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        LocalDate checkIn = booking.getCheckInDate();
        LocalDate checkOut = booking.getCheckOutDate();
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);

        logger.info("Calculating multi-night price: {} to {} ({} nights)",
                checkIn, checkOut, totalNights);

        // Get all matches happening during the stay
        List<Match> matchesDuringStay = matchRepository.findMatchesBetweenDates(
                checkIn.atStartOfDay(),
                checkOut.atTime(23, 59, 59)
        );

        if (matchesDuringStay.isEmpty()) {
            logger.info("No matches during stay, using base pricing");
            // No matches during stay, use base price
            return roomType.getBasePrice()
                    .multiply(BigDecimal.valueOf(totalNights))
                    .multiply(BigDecimal.valueOf(numberOfRooms));
        }

        logger.info("Found {} matches during stay period", matchesDuringStay.size());

        // Calculate price by tournament phases
        return calculatePhasedPrice(booking.getCheckInDate(),booking.getCheckOutDate(), hotel, roomType,
                numberOfRooms, matchesDuringStay);
    }

    /**
     * Calculate price by splitting stay into tournament phases
     */
    private BigDecimal calculatePhasedPrice(
           LocalDate checkIn,
            LocalDate checkOut,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms,
            List<Match> matches) {

        // Define tournament phases
        List<TournamentPhase> phases = defineTournamentPhases(matches);

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Calculate price for each phase that overlaps with booking
        for (TournamentPhase phase : phases) {
            // Find overlap between booking and phase
            LocalDate phaseStart = maxDate(checkIn, phase.getStartDate());
            LocalDate phaseEnd = minDate(checkOut, phase.getEndDate());

            if (!phaseStart.isBefore(phaseEnd)) {
                continue; // No overlap
            }

            long nightsInPhase = ChronoUnit.DAYS.between(phaseStart, phaseEnd);

            // Get most important match in this phase for pricing
            Match representativeMatch = getMostImportantMatchInPhase(phase, matches, hotel);

            // Get price for one night in this phase
            BigDecimal nightlyRate = basePricingService.calculateDynamicPrice(
                    roomType, hotel, representativeMatch, LocalDate.now(),
                    phase.getExpectedOccupancy());

            // Calculate total for this phase
            BigDecimal phaseTotal = nightlyRate
                    .multiply(BigDecimal.valueOf(nightsInPhase))
                    .multiply(BigDecimal.valueOf(numberOfRooms));

            logger.info("Phase '{}': {} nights × ${} × {} rooms = ${}",
                    phase.getName(), nightsInPhase, nightlyRate, numberOfRooms, phaseTotal);

            totalPrice = totalPrice.add(phaseTotal);
        }

        long totalNights = ChronoUnit.DAYS.between(
                checkIn, checkOut);
        BigDecimal avgNightlyRate = totalPrice
                .divide(BigDecimal.valueOf(totalNights * numberOfRooms), 2, RoundingMode.HALF_UP);

        logger.info("Total multi-night price: ${} ({} nights avg ${}/night per room)",
                totalPrice, totalNights, avgNightlyRate);

        return totalPrice;
    }

    /**
     * Define tournament phases based on actual match schedule
     */
    private List<TournamentPhase> defineTournamentPhases(List<Match> allMatches) {
        List<TournamentPhase> phases = new ArrayList<>();

        // Find date ranges for each tournament stage
        LocalDate groupStageStart = findFirstMatchDate(allMatches, Match.MatchStage.GROUP_STAGE_1);
        LocalDate groupStageEnd = findLastMatchDate(allMatches, Match.MatchStage.GROUP_STAGE_3);

        LocalDate round32Start = findFirstMatchDate(allMatches, Match.MatchStage.ROUND_OF_32);
        LocalDate round32End = findLastMatchDate(allMatches, Match.MatchStage.ROUND_OF_32);

        LocalDate round16Start = findFirstMatchDate(allMatches, Match.MatchStage.ROUND_OF_16);
        LocalDate round16End = findLastMatchDate(allMatches, Match.MatchStage.ROUND_OF_16);

        LocalDate quarterStart = findFirstMatchDate(allMatches, Match.MatchStage.QUARTER_FINAL);
        LocalDate quarterEnd = findLastMatchDate(allMatches, Match.MatchStage.QUARTER_FINAL);

        LocalDate semiStart = findFirstMatchDate(allMatches, Match.MatchStage.SEMI_FINAL);
        LocalDate semiEnd = findLastMatchDate(allMatches, Match.MatchStage.SEMI_FINAL);

        LocalDate finalDate = findFirstMatchDate(allMatches, Match.MatchStage.FINAL);

        // Create phases (with buffer days before/after for travel)
        if (groupStageStart != null && groupStageEnd != null) {
            phases.add(new TournamentPhase(
                    "Group Stage",
                    groupStageStart.minusDays(1),
                    groupStageEnd.plusDays(1),
                    65  // Expected 65% occupancy
            ));
        }

        if(round32Start != null && round32End != null) {
            phases.add(new TournamentPhase(
                    "Round of 32",
                    round32Start.minusDays(1),
                    round32End.plusDays(1),
                    70
            ));
        }

        if (round16Start != null && round16End != null) {
            phases.add(new TournamentPhase(
                    "Round of 16",
                    round16Start.minusDays(1),
                    round16End.plusDays(1),
                    75
            ));
        }

        if (quarterStart != null && quarterEnd != null) {
            phases.add(new TournamentPhase(
                    "Quarter Finals",
                    quarterStart.minusDays(1),
                    quarterEnd.plusDays(1),
                    85
            ));
        }

        if (semiStart != null && semiEnd != null) {
            phases.add(new TournamentPhase(
                    "Semi Finals",
                    semiStart.minusDays(1),
                    semiEnd.plusDays(1),
                    90
            ));
        }

        if (finalDate != null) {
            phases.add(new TournamentPhase(
                    "Finals Week",
                    finalDate.minusDays(3),
                    finalDate.plusDays(2),
                    95
            ));
        }

        return phases;
    }

    /**
     * Get the most important match in a tournament phase
     */
    private Match getMostImportantMatchInPhase(
            TournamentPhase phase,
            List<Match> allMatches,
            Hotel hotel) {

        // Filter to matches in this phase
        List<Match> phaseMatches = allMatches.stream()
                .filter(m -> {
                    LocalDate matchDate = m.getMatchDateTime().toLocalDate();
                    return !matchDate.isBefore(phase.getStartDate()) &&
                            !matchDate.isAfter(phase.getEndDate());
                })
                .toList();

        if (phaseMatches.isEmpty()) {
            // Create a default match if none found
            return createDefaultMatchForPhase(phase, hotel);
        }

        // Prefer matches in same city, then by importance
        return phaseMatches.stream()
                .max(Comparator
                        .comparing((Match m) -> m.getCity().equals(hotel.getCity()) ? 1 : 0)
                        .thenComparing(this::getMatchImportanceScore))
                .orElse(phaseMatches.get(0));
    }

    /**
     * Calculate importance score for a match
     */
    private int getMatchImportanceScore(Match match) {
        int score = switch (match.getStage()) {
            case FINAL -> 100;
            case SEMI_FINAL -> 80;
            case QUARTER_FINAL -> 60;
            case ROUND_OF_16 -> 40;
            case ROUND_OF_32 -> 35;
            case GROUP_STAGE_3 -> 30;
            case GROUP_STAGE_2 -> 20;
            case GROUP_STAGE_1 -> 10;
            default -> 0;
        };

        // Add bonuses
        if (match.isOpeningMatch()) score += 50;
        if (match.isDerby()) score += 30;


        return score;
    }

    /**
     * Find first match date for a given stage
     */
    private LocalDate findFirstMatchDate(List<Match> matches, Match.MatchStage stage) {
        return matches.stream()
                .filter(m -> m.getStage() == stage)
                .map(m -> m.getMatchDateTime().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Find last match date for a given stage
     */
    private LocalDate findLastMatchDate(List<Match> matches, Match.MatchStage stage) {
        return matches.stream()
                .filter(m -> m.getStage() == stage)
                .map(m -> m.getMatchDateTime().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Create a default match for a phase (fallback)
     */
    private Match createDefaultMatchForPhase(TournamentPhase phase, Hotel hotel) {
        Match defaultMatch = new Match();
        defaultMatch.setHomeTeam("TBD");
        defaultMatch.setAwayTeam("TBD");
        defaultMatch.setMatchDateTime(phase.getStartDate().atTime(15, 0));
        defaultMatch.setStage(Match.MatchStage.GROUP_STAGE_1);
        defaultMatch.setVenue("Stadium");
        defaultMatch.setCity(hotel.getCity());
        defaultMatch.getStadium().setStadiumLatitude(hotel.getLatitude());
        defaultMatch.getStadium().setStadiumLongitude(hotel.getLongitude());
        return defaultMatch;
    }

    /**
     * Get the later of two dates
     */
    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    /**
     * Get the earlier of two dates
     */
    private LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /**
     * Get detailed breakdown of multi-night pricing
     */
    public MultiNightPricingBreakdown getMultiNightBreakdown(
            Booking booking,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        List<Match> matches = matchRepository.findMatchesBetweenDates(
                booking.getCheckInDate().atStartOfDay(),
                booking.getCheckOutDate().atTime(23, 59, 59)
        );

        if (matches.isEmpty()) {
            return new MultiNightPricingBreakdown(
                    new ArrayList<>(),
                    roomType.getBasePrice(),
                    roomType.getBasePrice()
            );
        }

        List<TournamentPhase> phases = defineTournamentPhases(matches);
        List<PhaseBreakdown> phaseBreakdowns = new ArrayList<>();

        BigDecimal totalPrice = BigDecimal.ZERO;
        long totalNights = 0;

        for (TournamentPhase phase : phases) {
            LocalDate phaseStart = maxDate(booking.getCheckInDate(), phase.getStartDate());
            LocalDate phaseEnd = minDate(booking.getCheckOutDate(), phase.getEndDate());

            if (!phaseStart.isBefore(phaseEnd)) continue;

            long nightsInPhase = ChronoUnit.DAYS.between(phaseStart, phaseEnd);
            Match representativeMatch = getMostImportantMatchInPhase(phase, matches, hotel);

            BigDecimal nightlyRate = basePricingService.calculateDynamicPrice(
                    roomType, hotel, representativeMatch, LocalDate.now(),
                    phase.getExpectedOccupancy());

            BigDecimal phaseTotal = nightlyRate.multiply(BigDecimal.valueOf(nightsInPhase));

            phaseBreakdowns.add(new PhaseBreakdown(
                    phase.getName(),
                    phaseStart,
                    phaseEnd,
                    (int) nightsInPhase,
                    nightlyRate,
                    phaseTotal
            ));

            totalPrice = totalPrice.add(phaseTotal);
            totalNights += nightsInPhase;
        }

        BigDecimal avgRate = totalPrice.divide(
                BigDecimal.valueOf(totalNights), 2, RoundingMode.HALF_UP);

        return new MultiNightPricingBreakdown(phaseBreakdowns, totalPrice, avgRate);
    }

    // ============ Inner Classes ============

    /**
     * Represents a tournament phase
     */
    @Getter
    @AllArgsConstructor
    public static class TournamentPhase {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private int expectedOccupancy;
    }

    /**
     * Breakdown for one phase
     */
    @Getter
    @AllArgsConstructor
    public static class PhaseBreakdown {
        private String phaseName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int nights;
        private BigDecimal nightlyRate;
        private BigDecimal phaseTotal;
    }

    /**
     * Complete multi-night pricing breakdown
     */
    @Getter
    @AllArgsConstructor
    public static class MultiNightPricingBreakdown {
        private List<PhaseBreakdown> phases;
        private BigDecimal totalPrice;
        private BigDecimal averageNightlyRate;

        public String getExplanation() {
            if (phases.isEmpty()) {
                return "Standard pricing applied - no matches during stay";
            }

            StringBuilder sb = new StringBuilder("Multi-phase pricing:\n");
            for (PhaseBreakdown phase : phases) {
                sb.append(String.format("• %s (%s to %s): %d nights × $%.2f = $%.2f\n",
                        phase.getPhaseName(),
                        phase.getStartDate(),
                        phase.getEndDate(),
                        phase.getNights(),
                        phase.getNightlyRate(),
                        phase.getPhaseTotal()));
            }
            sb.append(String.format("\nTotal: $%.2f (avg $%.2f/night)",
                    totalPrice, averageNightlyRate));
            return sb.toString();
        }
    }
    public BigDecimal calculateTotalStayPrice(
            LocalDate checkIn,
            LocalDate checkOut,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);

        logger.info("Calculating multi-night price: {} to {} ({} nights)",
                checkIn, checkOut, totalNights);

        // Get all matches happening during the stay
        List<Match> matchesDuringStay = matchRepository.findMatchesBetweenDates(
                checkIn.atStartOfDay(),
                checkOut.atTime(23, 59, 59)
        );

        if (matchesDuringStay.isEmpty()) {
            logger.info("No matches during stay, using base pricing");
            // No matches during stay, use base price
            return roomType.getBasePrice()
                    .multiply(BigDecimal.valueOf(totalNights))
                    .multiply(BigDecimal.valueOf(numberOfRooms));
        }

        logger.info("Found {} matches during stay period", matchesDuringStay.size());

        // Calculate price by tournament phases
        return calculatePhasedPrice(checkIn,checkOut, hotel, roomType,
                numberOfRooms, matchesDuringStay);
    }
}
