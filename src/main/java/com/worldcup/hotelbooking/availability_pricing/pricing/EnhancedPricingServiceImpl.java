package com.worldcup.hotelbooking.availability_pricing.pricing;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enhanced pricing service that prices each night individually based on
 * the most important match happening on that night.
 * <p>
 * ─── HOW IT WORKS ────────────────────────────────────────────────────────────
 * <p>
 * For a stay of checkIn → checkOut we loop over every single night:
 * <p>
 * Night 1  (checkIn)      → find the best match on that date → price it
 * Night 2  (checkIn + 1)  → find the best match on that date → price it
 * ...
 * Night N  (checkOut - 1) → find the best match on that date → price it
 * <p>
 * "Best match on a date" = the match with the highest importance score
 * (FINAL > SEMI > QF > R16 > R32 > Group3 > Group2 > Group1).
 * <p>
 * If NO match falls on a particular night, we price that night at base rate
 * (no multiplier applied) so the total is NEVER zero.
 * <p>
 * This eliminates the old bug where calculatePhasedPrice() returned $0 for
 * dates that didn't overlap with any pre-defined TournamentPhase bucket.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
public class EnhancedPricingServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedPricingServiceImpl.class);

    private final PricingServiceImpl basePricingService;
    private final MatchRepository matchRepository;

    public EnhancedPricingServiceImpl(
            PricingServiceImpl basePricingService,
            MatchRepository matchRepository) {
        this.basePricingService = basePricingService;
        this.matchRepository = matchRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Main entry point called by BookingServiceImpl.calculateTotalPrice().
     * Accepts a Booking object and delegates to the date-based overload.
     */
    public BigDecimal calculateTotalStayPrice(
            Booking booking,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        return calculateTotalStayPrice(
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                hotel,
                roomType,
                numberOfRooms
        );
    }

    /**
     * Overload that works with raw dates (used by catalog/preview endpoints).
     * <p>
     * Prices EVERY SINGLE NIGHT individually:
     * - Nights with a match nearby → dynamic price based on match importance
     * - Nights with NO match       → base price per night (never $0)
     */
    public BigDecimal calculateTotalStayPrice(
            LocalDate checkIn,
            LocalDate checkOut,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (totalNights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        logger.info("Pricing stay: {} → {} ({} nights, {} rooms, hotel={})",
                checkIn, checkOut, totalNights, numberOfRooms, hotel.getName());

        // Load ALL matches that could be relevant to this stay.
        // We expand the window slightly so matches just before/after still influence pricing.
        List<Match> matchesDuringStay = matchRepository.findMatchesBetweenDates(
                checkIn.minusDays(1).atStartOfDay(),
                checkOut.plusDays(1).atTime(23, 59, 59)
        );

        logger.info("Found {} matches in/near stay window", matchesDuringStay.size());

        BigDecimal totalPrice = BigDecimal.ZERO;

        // ── Price each night individually ─────────────────────────────────────
        for (long i = 0; i < totalNights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightlyPrice = priceOneNight(night, hotel, roomType, matchesDuringStay);
            BigDecimal nightContribution = nightlyPrice.multiply(BigDecimal.valueOf(numberOfRooms));

            logger.debug("Night {}: {} → ${}/room × {} rooms = ${}",
                    i + 1, night, nightlyPrice, numberOfRooms, nightContribution);

            totalPrice = totalPrice.add(nightContribution);
        }

        BigDecimal result = totalPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgPerNightPerRoom = result.divide(
                BigDecimal.valueOf(totalNights * numberOfRooms), 2, RoundingMode.HALF_UP);

        logger.info("Total price: ${} | Avg: ${}/night/room over {} nights × {} rooms",
                result, avgPerNightPerRoom, totalNights, numberOfRooms);

        return result;
    }

    /**
     * Detailed breakdown for UI / API preview — shows each night's contribution.
     */
    public MultiNightPricingBreakdown getMultiNightBreakdown(
            Booking booking,
            Hotel hotel,
            RoomType roomType,
            int numberOfRooms) {

        LocalDate checkIn = booking.getCheckInDate();
        LocalDate checkOut = booking.getCheckOutDate();
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);

        List<Match> matches = matchRepository.findMatchesBetweenDates(
                checkIn.minusDays(1).atStartOfDay(),
                checkOut.plusDays(1).atTime(23, 59, 59)
        );

        // Group consecutive nights that share the same representative match
        // into PhaseBreakdown entries for a cleaner UI.
        List<PhaseBreakdown> phases = buildPhaseBreakdowns(
                checkIn, checkOut, hotel, roomType, matches);

        BigDecimal totalPrice = phases.stream()
                .map(PhaseBreakdown::getPhaseTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRate = totalNights == 0
                ? BigDecimal.ZERO
                : totalPrice.divide(BigDecimal.valueOf(totalNights), 2, RoundingMode.HALF_UP);

        return new MultiNightPricingBreakdown(phases, totalPrice, avgRate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE NIGHT PRICING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Price ONE night.
     * <p>
     * Strategy:
     * 1. Find all matches whose date == this night.
     * 2. If any exist, pick the most important one (highest importance score).
     * 3. Call basePricingService.calculateDynamicPrice() with that match.
     * 4. If NO match exists for this night, return roomType.getBasePrice()
     * (a safe, sensible fallback — never $0).
     */
    private BigDecimal priceOneNight(
            LocalDate night,
            Hotel hotel,
            RoomType roomType,
            List<Match> allMatches) {

        // Find matches on exactly this night
        List<Match> matchesOnNight = allMatches.stream()
                .filter(m -> m.getMatchDateTime().toLocalDate().equals(night))
                .toList();

        if (matchesOnNight.isEmpty()) {
            // ── No match tonight — charge base price, never $0 ────────────────
            logger.debug("Night {}: no match → base price ${}", night, roomType.getBasePrice());
            return roomType.getBasePrice();
        }

        // ── Pick the most important match (closest stadium first, then stage) ──
        Match best = matchesOnNight.stream()
                .max(Comparator
                        .comparingInt(this::getMatchImportanceScore)
                        .thenComparingInt(m -> isInSameCity(m, hotel) ? 1 : 0))
                .orElse(matchesOnNight.get(0));

        // Estimate current occupancy from match importance (simple heuristic)
        int estimatedOccupancy = estimateOccupancy(best);

        BigDecimal price = basePricingService.calculateDynamicPrice(
                roomType, hotel, best, LocalDate.now(), estimatedOccupancy);

        logger.debug("Night {}: match='{}' stage={} importance={} occupancy={}% → ${}",
                night,
                best.getHomeTeam() + " vs " + best.getAwayTeam(),
                best.getStage(),
                getMatchImportanceScore(best),
                estimatedOccupancy,
                price);

        return price;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMPORTANCE SCORING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Numeric importance score. Higher = more expensive night.
     * Used to pick the "best" match when multiple matches fall on the same night.
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
            default -> 5;
        };

        if (match.isOpeningMatch()) score += 50;
        if (match.isDerby()) score += 30;

        return score;
    }

    /**
     * Estimate expected occupancy percentage based on match importance.
     * Used as input to the demand multiplier inside PricingServiceImpl.
     */
    private int estimateOccupancy(Match match) {
        return switch (match.getStage()) {
            case FINAL -> 98;
            case SEMI_FINAL -> 93;
            case QUARTER_FINAL -> 87;
            case ROUND_OF_16 -> 78;
            case ROUND_OF_32 -> 72;
            case GROUP_STAGE_3 -> 68;
            case GROUP_STAGE_2 -> 60;
            case GROUP_STAGE_1 -> 55;
            default -> 50;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE BREAKDOWN (for UI display)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Groups consecutive nights that share the same match stage into
     * PhaseBreakdown entries so the API response is readable.
     * <p>
     * e.g. 5 Group-Stage nights at $120 → one entry instead of 5 identical rows.
     */
    private List<PhaseBreakdown> buildPhaseBreakdowns(
            LocalDate checkIn,
            LocalDate checkOut,
            Hotel hotel,
            RoomType roomType,
            List<Match> matches) {

        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        List<PhaseBreakdown> result = new ArrayList<>();

        // Track current run
        String currentLabel = null;
        LocalDate runStart = null;
        BigDecimal runRate = null;
        int runNights = 0;

        for (long i = 0; i < totalNights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal rate = priceOneNight(night, hotel, roomType, matches);
            String label = nightLabel(night, matches);

            if (label.equals(currentLabel) && rate.compareTo(runRate) == 0) {
                // Continue the current run
                runNights++;
            } else {
                // Flush previous run
                if (currentLabel != null) {
                    BigDecimal phaseTotal = runRate.multiply(BigDecimal.valueOf(runNights));
                    result.add(new PhaseBreakdown(
                            currentLabel, runStart, night.minusDays(1),
                            runNights, runRate, phaseTotal));
                }
                // Start new run
                currentLabel = label;
                runStart = night;
                runRate = rate;
                runNights = 1;
            }
        }

        // Flush final run
        if (currentLabel != null) {
            BigDecimal phaseTotal = runRate.multiply(BigDecimal.valueOf(runNights));
            result.add(new PhaseBreakdown(
                    currentLabel, runStart, checkOut,
                    runNights, runRate, phaseTotal));
        }

        return result;
    }

    /**
     * Human-readable label for a night (used to group runs in the breakdown).
     */
    private String nightLabel(LocalDate night, List<Match> matches) {
        return matches.stream()
                .filter(m -> m.getMatchDateTime().toLocalDate().equals(night))
                .max(Comparator.comparingInt(this::getMatchImportanceScore))
                .map(m -> stageLabel(m.getStage()))
                .orElse("No Match Night");
    }

    private String stageLabel(Match.MatchStage stage) {
        return switch (stage) {
            case FINAL -> "Final Night";
            case SEMI_FINAL -> "Semi-Final Night";
            case QUARTER_FINAL -> "Quarter-Final Night";
            case ROUND_OF_16 -> "Round of 16 Night";
            case ROUND_OF_32 -> "Round of 32 Night";
            case GROUP_STAGE_3 -> "Group Stage (Matchday 3) Night";
            case GROUP_STAGE_2 -> "Group Stage (Matchday 2) Night";
            case GROUP_STAGE_1 -> "Group Stage (Matchday 1) Night";
            default -> "Match Night";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isInSameCity(Match match, Hotel hotel) {
        String matchCity = match.getStadium() != null ? match.getStadium().getCity() : null;
        return matchCity != null && matchCity.equalsIgnoreCase(hotel.getCity());
    }

    /**
     * Creates a placeholder Match for nights that have no real match.
     * This is only used when explicitly needed (e.g. legacy callers); the
     * main priceOneNight() path returns base price directly without this.
     */
    private Match createFallbackMatch(LocalDate date, Hotel hotel) {
        Match m = new Match();
        m.setHomeTeam("TBD");
        m.setAwayTeam("TBD");
        m.setMatchDateTime(date.atTime(15, 0));
        m.setStage(Match.MatchStage.GROUP_STAGE_1);

        Stadium s = new Stadium();
        s.setName("Local Stadium");
        s.setCity(hotel.getCity());
        s.setLatitude(hotel.getLatitude());
        s.setLongitude(hotel.getLongitude());
        m.setStadium(s);
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INNER CLASSES (kept compatible with existing callers)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class TournamentPhase {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private int expectedOccupancy;
    }

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

    @Getter
    @AllArgsConstructor
    public static class MultiNightPricingBreakdown {
        private List<PhaseBreakdown> phases;
        private BigDecimal totalPrice;
        private BigDecimal averageNightlyRate;

        public String getExplanation() {
            if (phases.isEmpty()) {
                return "Standard base pricing applied — no matches during stay";
            }

            StringBuilder sb = new StringBuilder("Per-night pricing breakdown:\n");
            for (PhaseBreakdown phase : phases) {
                sb.append(String.format("• %s (%s → %s): %d night(s) × $%.2f = $%.2f%n",
                        phase.getPhaseName(),
                        phase.getStartDate(),
                        phase.getEndDate(),
                        phase.getNights(),
                        phase.getNightlyRate(),
                        phase.getPhaseTotal()));
            }
            sb.append(String.format("%nTotal: $%.2f  (avg $%.2f/night)",
                    totalPrice, averageNightlyRate));
            return sb.toString();
        }
    }
}