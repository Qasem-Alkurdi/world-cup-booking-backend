package com.worldcup.hotelbooking.availability_pricing.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PricingResponseDto {
    private BigDecimal basePrice;
    private double distanceMultiplier;
    private double matchMultiplier;
    private double demandMultiplier;
    private double timeMultiplier;
    private BigDecimal finalPrice;
    private double distanceFromStadium;

    public double getTotalMultiplier() {
        return distanceMultiplier * matchMultiplier * demandMultiplier * timeMultiplier;
    }

    public BigDecimal getSavings() {
        if (getTotalMultiplier() < 1.0) {
            return basePrice.subtract(finalPrice);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getPremium() {
        if (getTotalMultiplier() > 1.0) {
            return finalPrice.subtract(basePrice);
        }
        return BigDecimal.ZERO;
    }
}
