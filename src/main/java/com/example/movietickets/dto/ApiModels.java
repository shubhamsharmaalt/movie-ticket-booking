package com.example.movietickets.dto;

import com.example.movietickets.entity.BookingStatus;
import com.example.movietickets.entity.City;
import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.Notification;
import com.example.movietickets.entity.NotificationType;
import com.example.movietickets.entity.PaymentOutcome;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ApiModels {
    private ApiModels() {}

    public record CityInput(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 80) String timeZone) {}
    public record TheaterInput(@NotNull Long cityId, @NotBlank @Size(max = 120) String name) {}
    public record SeatInput(@NotBlank @Size(max = 20) String label, @NotNull SeatTier tier) {}
    public record MovieInput(@NotBlank @Size(max = 200) String title, @Positive int durationMinutes) {}
    public record PolicyInput(@NotBlank @Size(max = 120) String name, @PositiveOrZero int fullRefundHours,
                              @PositiveOrZero int partialRefundHours,
                              @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal partialPercent) {}
    public record DiscountInput(@NotBlank @Size(max = 40) String code,
                                @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal percent,
                                @NotNull Instant validFrom, @NotNull Instant validUntil, boolean active) {}
    public record ShowInput(@NotNull Long movieId, @NotNull Long theaterId, @NotNull Long refundPolicyId,
                            @NotNull Instant startsAt,
                            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal regularPrice,
                            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal premiumPrice,
                            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal weekendSurchargePercent) {}
    public record HoldInput(@NotNull Long showId, @NotEmpty @Size(max = 10) List<@NotNull Long> seatIds,
                            String discountCode) {}
    public record PaymentInput(@NotNull PaymentOutcome outcome) {}

    public record CityView(Long id, String name, String timeZone) {
        public static CityView of(City value) { return new CityView(value.getId(), value.getName(), value.getTimeZone()); }
    }
    public record TheaterView(Long id, Long cityId, String name) {
        public static TheaterView of(Theater value) { return new TheaterView(value.getId(), value.getCityId(), value.getName()); }
    }
    public record SeatView(Long id, Long theaterId, String label, SeatTier tier) {
        public static SeatView of(TheaterSeat value) { return new SeatView(value.getId(), value.getTheaterId(), value.getLabel(), value.getTier()); }
    }
    public record MovieView(Long id, String title, int durationMinutes) {
        public static MovieView of(Movie value) { return new MovieView(value.getId(), value.getTitle(), value.getDurationMinutes()); }
    }
    public record PolicyView(Long id, String name, int fullRefundHours, int partialRefundHours, BigDecimal partialPercent) {
        public static PolicyView of(RefundPolicy value) { return new PolicyView(value.getId(), value.getName(), value.getFullRefundHours(),
                value.getPartialRefundHours(), value.getPartialPercent()); }
    }
    public record DiscountView(Long id, String code, BigDecimal percent, Instant validFrom, Instant validUntil, boolean active) {
        public static DiscountView of(DiscountCode value) { return new DiscountView(value.getId(), value.getCode(), value.getPercent(),
                value.getValidFrom(), value.getValidUntil(), value.getActive()); }
    }
    public record ShowView(Long id, Long movieId, Long theaterId, Long refundPolicyId, Instant startsAt,
                           BigDecimal regularPrice, BigDecimal premiumPrice, BigDecimal weekendSurchargePercent) {
        public static ShowView of(MovieShow value) { return new ShowView(value.getId(), value.getMovieId(), value.getTheaterId(),
                value.getRefundPolicyId(), value.getStartsAt(), value.getRegularPrice(), value.getPremiumPrice(), value.getWeekendSurchargePercent()); }
    }
    public record AvailabilityView(Long seatId, String label, SeatTier tier, SeatStatus status, BigDecimal price) {}
    public record BookedSeatView(Long seatId, String label, BigDecimal price) {}
    public record BookingView(Long id, Long showId, String username, BookingStatus status, Instant expiresAt,
                              BigDecimal total, String discountCode, List<BookedSeatView> seats,
                              BigDecimal refundAmount, Instant createdAt) {}
    public record NotificationView(Long id, Long bookingId, NotificationType type, Instant deliveredAt) {
        public static NotificationView of(Notification value) {
            return new NotificationView(value.getId(), value.getBookingId(), value.getType(), value.getDeliveredAt());
        }
    }
    public record PageView<T>(List<T> content, long totalElements, int page, int size) {
        public static <T> PageView<T> of(Page<T> source) {
            return new PageView<>(source.getContent(), source.getTotalElements(),
                    source.getNumber(), source.getSize());
        }
    }
}
