package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels.BookingView;
import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.DiscountInput;
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
import com.example.movietickets.entity.NotificationType;
import com.example.movietickets.entity.PaymentOutcome;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.repository.PaymentRepository;
import com.example.movietickets.repository.RefundRepository;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CatalogService;
import com.example.movietickets.service.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class BookingRulesIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired BookingService bookings;
    @Autowired NotificationService notifications;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;

    @Test
    void discountPaymentReminderAndZeroRefundAreRecorded() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();
        City city = catalog.createCity(new CityInput("RulesCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "RulesTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("RulesMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("RulesPolicy" + suffix, 24, 2, new BigDecimal("50")));
        catalog.createDiscount(new DiscountInput("SAVE10" + suffix, new BigDecimal("10"),
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), true));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                now.plus(30, ChronoUnit.MINUTES), new BigDecimal("100.00"), new BigDecimal("150.00"), BigDecimal.ZERO));

        BookingView held = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), "SAVE10" + suffix), "alice");
        assertEquals(new BigDecimal("90.00"), held.total());
        assertEquals(BookingStatus.HELD, bookings.pay(held.id(), "alice", PaymentOutcome.FAILURE).status());
        assertThrows(ApiException.class, () -> bookings.get(held.id(), "bob"));
        assertEquals(BookingStatus.CONFIRMED, bookings.pay(held.id(), "alice", PaymentOutcome.SUCCESS).status());
        bookings.pay(held.id(), "alice", PaymentOutcome.SUCCESS);
        assertEquals(2, payments.findByBookingIdOrderByCreatedAtDesc(held.id()).size());

        notifications.enqueueReminders();
        notifications.deliverPending();
        assertEquals(2, notifications.inbox("alice", 0, 100).getContent().stream()
                .filter(notification -> notification.bookingId().equals(held.id())).count());
        BookingView cancelled = bookings.cancel(held.id(), "alice");
        assertEquals(new BigDecimal("0.00"), cancelled.refundAmount());
        bookings.cancel(held.id(), "alice");
        assertEquals(1, refunds.findByBookingId(held.id()).stream().count());
        assertEquals(SeatStatus.AVAILABLE, catalog.availability(show.getId()).getFirst().status());
    }

    @Test
    void refundPolicyIsFrozenWhenPaymentConfirmsTheBooking() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("PolicyCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "PolicyTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("PolicyMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("Policy" + suffix, 24, 2, new BigDecimal("50")));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                Instant.now().plus(3, ChronoUnit.HOURS), new BigDecimal("100.00"),
                new BigDecimal("150.00"), BigDecimal.ZERO));
        BookingView hold = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), null), "alice");
        catalog.updatePolicy(policy.getId(), new PolicyInput(policy.getName(), 4, 2, new BigDecimal("25")));
        bookings.pay(hold.id(), "alice", PaymentOutcome.SUCCESS);
        catalog.updatePolicy(policy.getId(), new PolicyInput(policy.getName(), 4, 2, new BigDecimal("100")));
        assertEquals(new BigDecimal("25.00"), bookings.cancel(hold.id(), "alice").refundAmount());
    }

    @Test
    void pendingReminderIsNotDeliveredAfterCancellation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("ReminderCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "ReminderTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("ReminderMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("ReminderPolicy" + suffix, 24, 2, new BigDecimal("50")));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                Instant.now().plus(30, ChronoUnit.MINUTES), new BigDecimal("100"),
                new BigDecimal("150"), BigDecimal.ZERO));
        BookingView hold = bookings.hold(new HoldInput(show.getId(), List.of(seat.getId()), null), "alice");
        bookings.pay(hold.id(), "alice", PaymentOutcome.SUCCESS);
        notifications.enqueueReminders();
        bookings.cancel(hold.id(), "alice");
        notifications.deliverPending();
        assertEquals(0, notifications.inbox("alice", 0, 100).getContent().stream()
                .filter(notification -> notification.bookingId().equals(hold.id())
                        && notification.type() == NotificationType.REMINDER).count());
    }

    @Test
    void multiSeatHoldIsAllOrNothingWhenOneSeatIsTaken() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new CityInput("AtomicCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new TheaterInput(city.getId(), "AtomicTheater"));
        TheaterSeat first = catalog.createSeat(theater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        TheaterSeat second = catalog.createSeat(theater.getId(), new SeatInput("A2", SeatTier.PREMIUM));
        Movie movie = catalog.createMovie(new MovieInput("AtomicMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("AtomicPolicy" + suffix, 24, 2, new BigDecimal("50")));
        MovieShow show = catalog.createShow(new ShowInput(movie.getId(), theater.getId(), policy.getId(),
                Instant.now().plus(1, ChronoUnit.DAYS), new BigDecimal("100"),
                new BigDecimal("150"), BigDecimal.ZERO));
        bookings.hold(new HoldInput(show.getId(), List.of(second.getId()), null), "bob");
        assertThrows(ApiException.class, () -> bookings.hold(new HoldInput(show.getId(),
                List.of(first.getId(), second.getId()), null), "alice"));
        assertEquals(SeatStatus.AVAILABLE, catalog.availability(show.getId()).getFirst().status());
        assertEquals(SeatStatus.HELD, catalog.availability(show.getId()).get(1).status());
    }
}
