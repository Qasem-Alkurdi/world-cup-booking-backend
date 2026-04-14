package com.worldcup.hotelbooking.availability_pricing.pricing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "worldcup.pricing")
@Getter
@Setter
public class PricingConfig {

    // Distance multipliers (in km)
    private DistanceMultipliers distance = new DistanceMultipliers();

    // Match stage multipliers
    private MatchMultipliers match = new MatchMultipliers();

    // Demand multipliers
    private DemandMultipliers demand = new DemandMultipliers();

    // Time multipliers
    private TimeMultipliers time = new TimeMultipliers();

    @Getter
    @Setter
    public static class DistanceMultipliers {
        private double walkingDistance = 2.5;      // 0-2 km
        private double shortDrive = 2.0;           // 2-5 km
        private double medium = 1.5;               // 5-10 km
        private double far = 1.2;                  // 10-20 km
        private double veryFar = 1.0;              // 20+ km
    }

    @Getter
    @Setter
    public static class MatchMultipliers {
        private double finalMatch = 3.5;
        private double semiFinal = 3.0;
        private double quarterFinal = 2.5;
        private double roundOf32 = 2.2;
        private double roundOf16 = 2.0;
        private double groupStage3 = 1.8;
        private double groupStage2 = 1.4;
        private double groupStage1 = 1.2;
        private double openingMatch = 2.8;

        // Bonuses
        private double popularTeamBonus = 0.3;
        private double derbyBonus = 0.5;
    }

    @Getter
    @Setter
    public static class DemandMultipliers {
        private double veryHighOccupancy = 1.8;    // 90-100%
        private double highOccupancy = 1.5;        // 75-89%
        private double mediumHighOccupancy = 1.3;  // 60-74%
        private double normalOccupancy = 1.1;      // 40-59%
        private double lowOccupancy = 1.0;         // 20-39%
        private double veryLowOccupancy = 0.9;     // 0-19%

        // Days before match
        private double lastMinute = 1.5;           // 0-7 days
        private double lateBooking = 1.3;          // 8-14 days
        private double standard = 1.0;             // 15-30 days
        private double earlyBird = 0.95;           // 31-60 days
        private double advanceBooking = 0.90;      // 61-90 days
        private double superEarlyBird = 0.85;      // 90+ days
    }

    @Getter
    @Setter
    public static class TimeMultipliers {
        private double weekend = 1.3;              // Fri-Sun
        private double preMatch = 1.2;             // Thursday
        private double weekday = 1.0;              // Mon-Wed

    }
}