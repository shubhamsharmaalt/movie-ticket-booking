package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.HoldInput;
import com.example.movietickets.dto.ApiModels.MovieInput;
import com.example.movietickets.dto.ApiModels.PolicyInput;
import com.example.movietickets.dto.ApiModels.SeatInput;
import com.example.movietickets.dto.ApiModels.ShowInput;
import com.example.movietickets.dto.ApiModels.TheaterInput;
import com.example.movietickets.entity.City;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CatalogService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentHoldIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired BookingService bookings;

    @Test
    void exactlyOneCustomerAcquiresContestedSeat() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("RaceCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "RaceTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("RaceMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("RacePolicy" + suffix, 24, 2, new BigDecimal("50")));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                Instant.now().plus(1, ChronoUnit.DAYS), new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO));
        HoldInput request = new HoldInput(show.getId(), List.of(seat.getId()), null);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> alice = executor.submit(() -> acquire(request, "alice", start));
            Future<Boolean> bob = executor.submit(() -> acquire(request, "bob", start));
            start.countDown();
            assertEquals(1, (alice.get() ? 1 : 0) + (bob.get() ? 1 : 0));
        }
    }

    private boolean acquire(HoldInput input, String username, CountDownLatch start) throws Exception {
        start.await();
        try { bookings.hold(input, username); return true; }
        catch (ApiException conflict) { return false; }
    }
}
