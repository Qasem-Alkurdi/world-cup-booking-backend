package com.worldcup.hotelbooking.availability_pricing.pricing;

import com.worldcup.hotelbooking.availability_pricing.match.Match;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class PricingService {

    private static final Logger logger = LoggerFactory.getLogger(PricingService.class);
    private static final List<String> POPULAR_TEAMS = Arrays.asList(
            "Brazil", "Argentina", "Germany", "France", "Spain",
            "England", "Italy", "Portugal", "Belgium"
    );

    private final PricingConfig config;

    public PricingService(PricingConfig config) {
        this.config = config;
    }

    /**
     * Calculate dynamic price for a room based on all factors
     */
    public BigDecimal calculateDynamicPrice(
            RoomType roomType,
            Hotel hotel,
            Match match,
            LocalDate bookingDate,
            int currentOccupancyPercent) {

        BigDecimal basePrice = roomType.getBasePrice();

        // Calculate all multipliers
        double distanceMultiplier = calculateDistanceMultiplier(hotel, match);
        double matchMultiplier = calculateMatchMultiplier(match);
        double demandMultiplier = calculateDemandMultiplier(
                match.getMatchDateTime().toLocalDate(),
                bookingDate,
                currentOccupancyPercent
        );
        double timeMultiplier = calculateTimeMultiplier(match);

        // Combine all multipliers
        double totalMultiplier = distanceMultiplier * matchMultiplier *
                demandMultiplier * timeMultiplier;

        // Calculate final price
        BigDecimal finalPrice = basePrice.multiply(BigDecimal.valueOf(totalMultiplier))
                .setScale(2, RoundingMode.HALF_UP);// Round to 2 decimal places

        // Log pricing breakdown for transparency
        logger.info("Pricing breakdown for {} - Base: ${}, Distance: {}x, Match: {}x, " +
                        "Demand: {}x, Time: {}x, Final: ${}",
                roomType.getName(), basePrice, distanceMultiplier, matchMultiplier,
                demandMultiplier, timeMultiplier, finalPrice);

        return finalPrice;
    }

    /**
     * Calculate distance-based multiplier
     */
    private double calculateDistanceMultiplier(Hotel hotel, Match match) {
        double distance = calculateDistance(
                hotel.getLatitude(), hotel.getLongitude(),
                match.getStadium().getLatitude(), match.getStadium().getLongitude()
        );

        if (distance <= 2) {
            return config.getDistance().getWalkingDistance();
        } else if (distance <= 5) {
            return config.getDistance().getShortDrive();
        } else if (distance <= 10) {
            return config.getDistance().getMedium();
        } else if (distance <= 20) {
            return config.getDistance().getFar();
        } else {
            return config.getDistance().getVeryFar();
        }
    }

    /**
     * Calculate match importance multiplier
     */
    private double calculateMatchMultiplier(Match match) {
        double baseMultiplier = getBaseMatchMultiplier(match.getStage());

        // Add opening match bonus
        if (match.isOpeningMatch()) {
            return config.getMatch().getOpeningMatch();
        }

        // Add team popularity bonuses
        double teamBonus = 0.0;

        // Check for popular teams
        if (isPopularTeam(match.getHomeTeam()) || isPopularTeam(match.getAwayTeam())) {
            teamBonus += config.getMatch().getPopularTeamBonus();
        }


        // Check for derby/rivalry
        if (match.isDerby()) {
            teamBonus += config.getMatch().getDerbyBonus();
        }

        return baseMultiplier + teamBonus;
    }

    /**
     * Get base multiplier based on match stage
     */
    private double getBaseMatchMultiplier(Match.MatchStage stage) {
        return switch (stage) {
            case FINAL -> config.getMatch().getFinalMatch();
            case SEMI_FINAL -> config.getMatch().getSemiFinal();
            case QUARTER_FINAL -> config.getMatch().getQuarterFinal();
            case ROUND_OF_16 -> config.getMatch().getRoundOf16();
            case GROUP_STAGE_3 -> config.getMatch().getGroupStage3();
            case GROUP_STAGE_2 -> config.getMatch().getGroupStage2();
            case GROUP_STAGE_1 -> config.getMatch().getGroupStage1();
            default -> 1.0;
        };
    }

    /**
     * Calculate demand-based multiplier
     */
    private double calculateDemandMultiplier(
            LocalDate matchDate,
            LocalDate bookingDate,
            int occupancyPercent) {

        // Occupancy-based multiplier
        double occupancyMultiplier = getOccupancyMultiplier(occupancyPercent);

        // Days-before-match multiplier
        long daysUntilMatch = ChronoUnit.DAYS.between(bookingDate, matchDate);
        double daysMultiplier = getDaysBeforeMatchMultiplier(daysUntilMatch);

        // Combine (average to avoid extreme pricing)
        return (occupancyMultiplier + daysMultiplier) / 2.0;
    }

    /**
     * Get multiplier based on occupancy percentage
     */
    private double getOccupancyMultiplier(int occupancyPercent) {
        if (occupancyPercent >= 90) {
            return config.getDemand().getVeryHighOccupancy();
        } else if (occupancyPercent >= 75) {
            return config.getDemand().getHighOccupancy();
        } else if (occupancyPercent >= 60) {
            return config.getDemand().getMediumHighOccupancy();
        } else if (occupancyPercent >= 40) {
            return config.getDemand().getNormalOccupancy();
        } else if (occupancyPercent >= 20) {
            return config.getDemand().getLowOccupancy();
        } else {
            return config.getDemand().getVeryLowOccupancy();
        }
    }

    /**
     * Get multiplier based on days before match
     */
    private double getDaysBeforeMatchMultiplier(long days) {
        if (days <= 7) {
            return config.getDemand().getLastMinute();
        } else if (days <= 14) {
            return config.getDemand().getLateBooking();
        } else if (days <= 30) {
            return config.getDemand().getStandard();
        } else if (days <= 60) {
            return config.getDemand().getEarlyBird();
        } else if (days <= 90) {
            return config.getDemand().getAdvanceBooking();
        } else {
            return config.getDemand().getSuperEarlyBird();
        }
    }

    /**
     * Calculate time-based multiplier (day of week, tournament phase)
     */
    private double calculateTimeMultiplier(Match match) {
        LocalDate matchDate = match.getMatchDateTime().toLocalDate();

        // Day of week multiplier
        double dayMultiplier = switch (matchDate.getDayOfWeek()) {
            case FRIDAY, SATURDAY, SUNDAY -> config.getTime().getWeekend();
            case THURSDAY -> config.getTime().getPreMatch();
            default -> config.getTime().getWeekday();
        };


        // Combine (multiply to compound the effects)
        return dayMultiplier ;
    }

    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Check if team is in popular teams list
     */
    private boolean isPopularTeam(String team) {
        return POPULAR_TEAMS.contains(team);
    }

    /**
     * Check if team is a host nation
     */


    /**
     * Get pricing breakdown for transparency
     */
    public PricingResponseDto getPricingBreakdown(
            RoomType roomType,
            Hotel hotel,
            Match match,
            LocalDate bookingDate,
            int currentOccupancyPercent) {

        BigDecimal basePrice = roomType.getBasePrice();
        double distanceMultiplier = calculateDistanceMultiplier(hotel, match);
        double matchMultiplier = calculateMatchMultiplier(match);
        double demandMultiplier = calculateDemandMultiplier(
                match.getMatchDateTime().toLocalDate(), bookingDate, currentOccupancyPercent);
        double timeMultiplier = calculateTimeMultiplier(match);

        BigDecimal finalPrice = calculateDynamicPrice(
                roomType, hotel, match, bookingDate, currentOccupancyPercent);

        return new PricingResponseDto(
                basePrice,
                distanceMultiplier,
                matchMultiplier,
                demandMultiplier,
                timeMultiplier,
                finalPrice,
                calculateDistance(hotel.getLatitude(), hotel.getLongitude(),
                        match.getStadium().getLatitude(), match.getStadium().getLongitude())
        );
    }
}