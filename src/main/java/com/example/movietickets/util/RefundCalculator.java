package com.example.movietickets.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public final class RefundCalculator {
    private RefundCalculator() {}

    public static BigDecimal amount(BigDecimal paid, Instant cancelledAt, Instant showAt,
                             int fullHours, int partialHours, BigDecimal partialPercent) {
        Duration remaining = Duration.between(cancelledAt, showAt);
        BigDecimal percent = remaining.compareTo(Duration.ofHours(fullHours)) >= 0 ? new BigDecimal("100")
                : remaining.compareTo(Duration.ofHours(partialHours)) >= 0 ? partialPercent : BigDecimal.ZERO;
        return paid.multiply(percent.movePointLeft(2)).setScale(2, RoundingMode.HALF_UP);
    }
}
