package com.example.movietickets.util;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundCalculatorTest {
    private static final Instant SHOW = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void appliesFullPartialAndZeroRefundAtCutoffs() {
        BigDecimal paid = new BigDecimal("120.00");
        assertEquals(new BigDecimal("120.00"), RefundCalculator.amount(paid, SHOW.minusSeconds(24 * 3600), SHOW, 24, 2, new BigDecimal("50")));
        assertEquals(new BigDecimal("60.00"), RefundCalculator.amount(paid, SHOW.minusSeconds(2 * 3600), SHOW, 24, 2, new BigDecimal("50")));
        assertEquals(new BigDecimal("0.00"), RefundCalculator.amount(paid, SHOW.minusSeconds(3600), SHOW, 24, 2, new BigDecimal("50")));
    }
}
