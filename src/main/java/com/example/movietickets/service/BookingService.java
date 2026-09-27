package com.example.movietickets.service;

import com.example.movietickets.dto.ApiModels.BookedSeatView;
import com.example.movietickets.dto.ApiModels.BookingView;
import com.example.movietickets.dto.ApiModels.HoldInput;
import com.example.movietickets.entity.Booking;
import com.example.movietickets.entity.BookingSeat;
import com.example.movietickets.entity.BookingStatus;
import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.NotificationType;
import com.example.movietickets.entity.Payment;
import com.example.movietickets.entity.PaymentOutcome;
import com.example.movietickets.entity.Refund;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.ShowSeat;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.repository.BookingSeatRepository;
import com.example.movietickets.repository.PaymentRepository;
import com.example.movietickets.repository.RefundRepository;
import com.example.movietickets.repository.ShowSeatRepository;
import com.example.movietickets.util.PricingCalculator;
import com.example.movietickets.util.RefundCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final CatalogService catalog;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final ShowSeatRepository showSeats;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final NotificationService notifications;
    private final Clock clock;
    private final long holdMinutes;

    public BookingService(CatalogService catalog, BookingRepository bookings, BookingSeatRepository bookingSeats,
                   ShowSeatRepository showSeats, PaymentRepository payments, RefundRepository refunds,
                   NotificationService notifications, Clock clock, @Value("${app.hold-minutes}") long holdMinutes) {
        this.catalog = catalog; this.bookings = bookings; this.bookingSeats = bookingSeats;
        this.showSeats = showSeats; this.payments = payments; this.refunds = refunds;
        this.notifications = notifications; this.clock = clock; this.holdMinutes = holdMinutes;
    }

    @Transactional
    public BookingView hold(HoldInput input, String username) {
        if (input.seatIds().size() != new HashSet<>(input.seatIds()).size())
            throw ApiException.bad("Seat IDs must be distinct");
        MovieShow show = catalog.lockShow(input.showId());
        Instant now = clock.instant();
        if (!show.getStartsAt().isAfter(now)) throw ApiException.conflict("Show has started");
        RefundPolicy policy = catalog.policy(show.getRefundPolicyId());
        Theater theater = catalog.theater(show.getTheaterId());
        ZoneId zone = ZoneId.of(catalog.city(theater.getCityId()).getTimeZone());
        List<Long> orderedIds = input.seatIds().stream().sorted().toList();
        List<ShowSeat> rows = new ArrayList<>();
        List<TheaterSeat> selected = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Long seatId : orderedIds) {
            ShowSeat row = showSeats.lockOne(show.getId(), seatId)
                    .orElseThrow(() -> ApiException.bad("Seat does not belong to this show"));
            if (row.getStatus() == SeatStatus.BOOKED ||
                    (row.getStatus() == SeatStatus.HELD && row.getHoldExpiresAt().isAfter(now)))
                throw ApiException.conflict("Seat is unavailable: " + seatId);
            TheaterSeat seat = catalog.seat(seatId);
            BigDecimal base = seat.getTier() == SeatTier.REGULAR ? show.getRegularPrice() : show.getPremiumPrice();
            BigDecimal price = PricingCalculator.seatPrice(base, show.getWeekendSurchargePercent(), show.getStartsAt(), zone);
            rows.add(row); selected.add(seat); prices.add(price); subtotal = subtotal.add(price);
        }
        String code = null;
        BigDecimal discountPercent = BigDecimal.ZERO;
        if (input.discountCode() != null && !input.discountCode().isBlank()) {
            DiscountCode discount = catalog.discountByCode(input.discountCode().trim());
            if (!discount.getActive() || now.isBefore(discount.getValidFrom()) || !now.isBefore(discount.getValidUntil()))
                throw ApiException.bad("Discount code is not active");
            code = discount.getCode(); discountPercent = discount.getPercent();
        }
        Instant candidateExpiry = now.plus(holdMinutes, ChronoUnit.MINUTES);
        Instant expiresAt = candidateExpiry.isBefore(show.getStartsAt()) ? candidateExpiry : show.getStartsAt();
        Booking booking = bookings.save(new Booking(show.getId(), username, expiresAt,
                PricingCalculator.applyDiscount(subtotal, discountPercent), code, policy, now));
        for (int i = 0; i < rows.size(); i++) {
            ShowSeat row = rows.get(i);
            row.setStatus(SeatStatus.HELD); row.setBookingId(booking.getId()); row.setHoldExpiresAt(expiresAt);
            bookingSeats.save(new BookingSeat(booking.getId(), selected.get(i).getId(), selected.get(i).getLabel(), prices.get(i)));
        }
        return view(booking);
    }

    @Transactional
    public BookingView pay(Long bookingId, String username, PaymentOutcome outcome) {
        Booking booking = ownedLocked(bookingId, username);
        if (booking.getStatus() == BookingStatus.CONFIRMED && outcome == PaymentOutcome.SUCCESS) return view(booking);
        if (booking.getStatus() != BookingStatus.HELD) throw ApiException.conflict("Booking is not awaiting payment");
        Instant now = clock.instant();
        if (!booking.getExpiresAt().isAfter(now)) throw ApiException.conflict("Hold has expired");
        MovieShow show = catalog.show(booking.getShowId());
        if (!show.getStartsAt().isAfter(now)) throw ApiException.conflict("Show has started");
        List<ShowSeat> rows = showSeats.lockByBooking(booking.getId());
        if (rows.size() != bookingSeats.findByBookingIdOrderBySeatId(booking.getId()).size()
                || rows.stream().anyMatch(row -> row.getStatus() != SeatStatus.HELD))
            throw ApiException.conflict("Hold no longer owns all seats");
        payments.save(new Payment(booking.getId(), outcome, booking.getTotal(), UUID.randomUUID().toString(), now));
        if (outcome == PaymentOutcome.SUCCESS) {
            RefundPolicy policy = catalog.policy(show.getRefundPolicyId());
            booking.setFullRefundHours(policy.getFullRefundHours());
            booking.setPartialRefundHours(policy.getPartialRefundHours());
            booking.setPartialPercent(policy.getPartialPercent());
            booking.setStatus(BookingStatus.CONFIRMED); booking.setConfirmedAt(now);
            rows.forEach(row -> { row.setStatus(SeatStatus.BOOKED); row.setHoldExpiresAt(null); });
            notifications.enqueue(booking, NotificationType.CONFIRMATION);
        }
        return view(booking);
    }

    @Transactional
    public BookingView cancel(Long bookingId, String username) {
        Booking booking = ownedLocked(bookingId, username);
        if (booking.getStatus() == BookingStatus.CANCELLED) return view(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED) throw ApiException.conflict("Only confirmed bookings can be cancelled");
        Instant now = clock.instant();
        MovieShow show = catalog.show(booking.getShowId());
        if (!now.isBefore(show.getStartsAt())) throw ApiException.conflict("Show has started");
        List<ShowSeat> rows = showSeats.lockByBooking(booking.getId());
        BigDecimal amount = RefundCalculator.amount(booking.getTotal(), now, show.getStartsAt(),
                booking.getFullRefundHours(), booking.getPartialRefundHours(), booking.getPartialPercent());
        refunds.save(new Refund(booking.getId(), amount, now));
        booking.setStatus(BookingStatus.CANCELLED);
        rows.forEach(row -> { row.setStatus(SeatStatus.AVAILABLE); row.setBookingId(null); row.setHoldExpiresAt(null); });
        notifications.enqueue(booking, NotificationType.CANCELLATION);
        return view(booking);
    }

    @Transactional
    public void expireOne(Long bookingId) {
        Booking booking = bookings.lockById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.HELD || booking.getExpiresAt().isAfter(clock.instant())) return;
        List<ShowSeat> rows = showSeats.lockByBooking(booking.getId());
        booking.setStatus(BookingStatus.EXPIRED);
        rows.forEach(row -> { row.setStatus(SeatStatus.AVAILABLE); row.setBookingId(null); row.setHoldExpiresAt(null); });
    }

    public BookingView get(Long id, String username) { return view(owned(id, username)); }

    public Page<BookingView> history(String username, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.bad("Invalid pagination");
        return bookings.findByUsernameOrderByCreatedAtDesc(username, PageRequest.of(page, size)).map(this::view);
    }

    private Booking owned(Long id, String username) {
        Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.missing("Booking not found"));
        if (!booking.getUsername().equals(username)) throw ApiException.forbidden("Booking belongs to another customer");
        return booking;
    }

    private Booking ownedLocked(Long id, String username) {
        Booking booking = bookings.lockById(id).orElseThrow(() -> ApiException.missing("Booking not found"));
        if (!booking.getUsername().equals(username)) throw ApiException.forbidden("Booking belongs to another customer");
        return booking;
    }

    private BookingView view(Booking booking) {
        List<BookedSeatView> seats = bookingSeats.findByBookingIdOrderBySeatId(booking.getId()).stream()
                .map(seat -> new BookedSeatView(seat.getSeatId(), seat.getLabel(), seat.getPrice())).toList();
        BigDecimal refund = refunds.findByBookingId(booking.getId()).map(value -> value.getAmount()).orElse(null);
        return new BookingView(booking.getId(), booking.getShowId(), booking.getUsername(), booking.getStatus(), booking.getExpiresAt(),
                booking.getTotal(), booking.getDiscountCode(), seats, refund, booking.getCreatedAt());
    }
}
