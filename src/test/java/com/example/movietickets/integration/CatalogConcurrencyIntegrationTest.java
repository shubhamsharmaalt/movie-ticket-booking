package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels.CityInput;
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
import com.example.movietickets.error.ApiException;
import com.example.movietickets.service.CatalogService;
import java.math.BigDecimal;
import java.time.Instant;
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
class CatalogConcurrencyIntegrationTest {
    @Autowired CatalogService catalog;

    @Test
    void concurrentShowCreationAllowsOnlyOneOverlappingShow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("ShowRaceCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "ShowRaceTheater"));
        catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("ShowRaceMovie", 120));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("ShowRacePolicy" + suffix, 24, 2, new BigDecimal("50")));
        ShowInput request = new ShowInput(movie.getId(), theater.getId(), policy.getId(), Instant.parse("2030-11-02T12:00:00Z"),
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = pool.submit(() -> createShow(request, start));
            Future<Boolean> second = pool.submit(() -> createShow(request, start));
            start.countDown();
            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
        }
    }

    @Test
    void showSeatSnapshotMatchesTheaterLayoutAfterConcurrentEdit() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("LayoutRaceCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "LayoutRaceTheater"));
        catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("LayoutRaceMovie", 120));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("LayoutRacePolicy" + suffix, 24, 2, new BigDecimal("50")));
        ShowInput request = new ShowInput(movie.getId(), theater.getId(), policy.getId(), Instant.parse("2030-11-03T12:00:00Z"),
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<MovieShow> showFuture = pool.submit(() -> { start.await(); return catalog.createShow(request); });
            Future<Boolean> seatFuture = pool.submit(() -> {
                start.await();
                try { catalog.createSeat(theater.getId(), new SeatInput("A2", SeatTier.PREMIUM)); return true; }
                catch (ApiException conflict) { return false; }
            });
            start.countDown();
            MovieShow show = showFuture.get();
            boolean seatAdded = seatFuture.get();
            assertEquals(seatAdded ? 2 : 1, catalog.availability(show.getId()).size());
        }
    }

    private boolean createShow(ShowInput request, CountDownLatch start) throws Exception {
        start.await();
        try { catalog.createShow(request); return true; }
        catch (ApiException conflict) { return false; }
    }
}
