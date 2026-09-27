package com.example.movietickets.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

public final class PricingCalculator {
    private PricingCalculator() {}

    public static BigDecimal seatPrice(BigDecimal base, BigDecimal weekendPercent, Instant showAt, ZoneId zone) {
        DayOfWeek day = showAt.atZone(zone).getDayOfWeek();
        BigDecimal price = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                ? base.multiply(BigDecimal.ONE.add(weekendPercent.movePointLeft(2))) : base;
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal applyDiscount(BigDecimal subtotal, BigDecimal percent) {
        return subtotal.multiply(BigDecimal.ONE.subtract(percent.movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
