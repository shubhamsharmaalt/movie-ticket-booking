package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels.BookingView;
import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.HoldInput;
import com.example.movietickets.dto.ApiModels.MovieInput;
import com.example.movietickets.dto.ApiModels.PolicyInput;
import com.example.movietickets.dto.ApiModels.SeatInput;
import com.example.movietickets.dto.ApiModels.ShowInput;
import com.example.movietickets.dto.ApiModels.TheaterInput;
import com.example.movietickets.entity.BookingStatus;
import com.example.movietickets.entity.City;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.PaymentOutcome;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CatalogService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(HoldExpiryIntegrationTest.ClockConfiguration.class)
class HoldExpiryIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired BookingService bookings;
    @Autowired MutableClock clock;

    @Test
    void holdNeverExtendsBeyondShowStart() {
        Instant now = Instant.parse("2030-10-01T10:00:00Z");
        clock.set(now);
        City city = catalog.createCity(new CityInput("ExpiryCity" + UUID.randomUUID().toString().substring(0, 8), "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "ExpiryTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("ExpiryMovie", 120));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("ExpiryPolicy", 24, 2, new BigDecimal("50")));
        Instant showAt = now.plusSeconds(120);
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(), showAt,
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO));

        BookingView hold = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), null), "alice");
        assertEquals(showAt, hold.expiresAt());
        clock.set(showAt);
        assertThrows(ApiException.class, () -> bookings.pay(hold.id(), "alice", PaymentOutcome.SUCCESS));
    }

    @Test
    void expiredHoldCanBeReclaimedBeforeCleanupRuns() {
        Instant now = Instant.parse("2030-10-02T10:00:00Z");
        clock.set(now);
        City city = catalog.createCity(new CityInput("ReclaimCity" + UUID.randomUUID().toString().substring(0, 8), "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "ReclaimTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("ReclaimMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("ReclaimPolicy" + city.getId(), 24, 2, new BigDecimal("50")));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                now.plusSeconds(3600), new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO));

        BookingView alice = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), null), "alice");
        clock.set(alice.expiresAt());
        assertEquals(SeatStatus.AVAILABLE, catalog.availability(show.getId()).getFirst().status());
        BookingView bob = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), null), "bob");
        bookings.expireOne(alice.id());
        assertEquals(BookingStatus.EXPIRED, bookings.get(alice.id(), "alice").status());
        assertEquals(BookingStatus.HELD, bookings.get(bob.id(), "bob").status());
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.EPOCH);
        void set(Instant instant) { current.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current.get(); }
    }

    @TestConfiguration
    static class ClockConfiguration {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }
}
