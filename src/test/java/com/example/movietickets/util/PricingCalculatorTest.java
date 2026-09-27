package com.example.movietickets.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingCalculatorTest {
    @Test
    void weekendUsesCityTimeZoneAndRoundsMoney() {
        Instant fridayUtcSaturdayIndia = Instant.parse("2026-10-02T19:00:00Z");
        assertEquals(new BigDecimal("125.00"), PricingCalculator.seatPrice(
                new BigDecimal("100.00"), new BigDecimal("25"), fridayUtcSaturdayIndia, ZoneId.of("Asia/Kolkata")));
        assertEquals(new BigDecimal("100.00"), PricingCalculator.seatPrice(
                new BigDecimal("100.00"), new BigDecimal("25"), fridayUtcSaturdayIndia, ZoneId.of("America/New_York")));
    }

    @Test
    void discountRoundsHalfUp() {
        assertEquals(new BigDecimal("66.67"), PricingCalculator.applyDiscount(
                new BigDecimal("100.00"), new BigDecimal("33.33")));
    }
}
