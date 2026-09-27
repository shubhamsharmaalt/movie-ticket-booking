package com.example.movietickets.service;

import com.example.movietickets.dto.ApiModels.AvailabilityView;
import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.DiscountInput;
import com.example.movietickets.dto.ApiModels.MovieInput;
import com.example.movietickets.dto.ApiModels.PolicyInput;
import com.example.movietickets.dto.ApiModels.SeatInput;
import com.example.movietickets.dto.ApiModels.ShowInput;
import com.example.movietickets.dto.ApiModels.TheaterInput;
import com.example.movietickets.entity.City;
import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.ShowSeat;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.repository.CityRepository;
import com.example.movietickets.repository.DiscountCodeRepository;
import com.example.movietickets.repository.MovieRepository;
import com.example.movietickets.repository.MovieShowRepository;
import com.example.movietickets.repository.RefundPolicyRepository;
import com.example.movietickets.repository.ShowSeatRepository;
import com.example.movietickets.repository.TheaterRepository;
import com.example.movietickets.repository.TheaterSeatRepository;
import com.example.movietickets.util.PricingCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CityRepository cities;
    private final TheaterRepository theaters;
    private final TheaterSeatRepository seats;
    private final MovieRepository movies;
    private final RefundPolicyRepository policies;
    private final DiscountCodeRepository discounts;
    private final MovieShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final BookingRepository bookings;
    private final Clock clock;

    public CatalogService(CityRepository cities, TheaterRepository theaters, TheaterSeatRepository seats,
                   MovieRepository movies, RefundPolicyRepository policies, DiscountCodeRepository discounts,
                   MovieShowRepository shows, ShowSeatRepository showSeats, BookingRepository bookings, Clock clock) {
        this.cities = cities; this.theaters = theaters; this.seats = seats; this.movies = movies;
        this.policies = policies; this.discounts = discounts; this.shows = shows; this.showSeats = showSeats;
        this.bookings = bookings; this.clock = clock;
    }

    public City city(Long id) { return cities.findById(id).orElseThrow(() -> ApiException.missing("City not found")); }
    private City lockCity(Long id) { return cities.lockById(id).orElseThrow(() -> ApiException.missing("City not found")); }
    public Theater theater(Long id) { return theaters.findById(id).orElseThrow(() -> ApiException.missing("Theater not found")); }
    private Theater lockTheater(Long id) { return theaters.lockById(id).orElseThrow(() -> ApiException.missing("Theater not found")); }
    public Movie movie(Long id) { return movies.findById(id).orElseThrow(() -> ApiException.missing("Movie not found")); }
    public RefundPolicy policy(Long id) { return policies.findById(id).orElseThrow(() -> ApiException.missing("Refund policy not found")); }
    public MovieShow show(Long id) { return shows.findById(id).orElseThrow(() -> ApiException.missing("Show not found")); }
    public MovieShow lockShow(Long id) { return shows.lockById(id).orElseThrow(() -> ApiException.missing("Show not found")); }
    public TheaterSeat seat(Long id) { return seats.findById(id).orElseThrow(() -> ApiException.missing("Seat not found")); }
    public DiscountCode discount(Long id) { return discounts.findById(id).orElseThrow(() -> ApiException.missing("Discount code not found")); }
    public DiscountCode discountByCode(String code) {
        return discounts.findByCodeIgnoreCase(code).orElseThrow(() -> ApiException.bad("Discount code is invalid"));
    }

    public City createCity(CityInput input) {
        ZoneId.of(input.timeZone());
        return cities.save(new City(input.name().trim(), input.timeZone()));
    }

    @Transactional public City updateCity(Long id, CityInput input) {
        ZoneId.of(input.timeZone());
        City city = lockCity(id);
        boolean hasShows = theaters.findAll().stream().filter(t -> t.getCityId().equals(id))
                .anyMatch(t -> shows.existsByTheaterId(t.getId()));
        if (hasShows && !city.getTimeZone().equals(input.timeZone()))
            throw ApiException.conflict("City time zone cannot change after show creation");
        city.setName(input.name().trim()); city.setTimeZone(input.timeZone());
        return city;
    }

    @Transactional public void deleteCity(Long id) {
        lockCity(id);
        if (theaters.existsByCityId(id)) throw ApiException.conflict("City has theaters");
        cities.deleteById(id);
    }

    @Transactional public Theater createTheater(TheaterInput input) {
        lockCity(input.cityId());
        return theaters.save(new Theater(input.cityId(), input.name().trim()));
    }

    @Transactional public Theater updateTheater(Long id, TheaterInput input) {
        Theater theater = lockTheater(id);
        city(input.cityId());
        if (shows.existsByTheaterId(id) && !theater.getCityId().equals(input.cityId()))
            throw ApiException.conflict("Theater city cannot change after show creation");
        theater.setCityId(input.cityId()); theater.setName(input.name().trim());
        return theater;
    }

    @Transactional public void deleteTheater(Long id) {
        lockTheater(id);
        if (shows.existsByTheaterId(id) || seats.existsByTheaterId(id))
            throw ApiException.conflict("Theater has shows or seats");
        theaters.deleteById(id);
    }

    @Transactional public TheaterSeat createSeat(Long theaterId, SeatInput input) {
        lockTheater(theaterId);
        requireEditableLayout(theaterId);
        return seats.save(new TheaterSeat(theaterId, input.label().trim().toUpperCase(), input.tier()));
    }

    @Transactional public TheaterSeat updateSeat(Long theaterId, Long seatId, SeatInput input) {
        lockTheater(theaterId);
        TheaterSeat seat = seat(seatId);
        if (!seat.getTheaterId().equals(theaterId)) throw ApiException.missing("Seat not found in theater");
        requireEditableLayout(theaterId);
        seat.setLabel(input.label().trim().toUpperCase()); seat.setTier(input.tier());
        return seat;
    }

    @Transactional public void deleteSeat(Long theaterId, Long seatId) {
        lockTheater(theaterId);
        TheaterSeat seat = seat(seatId);
        if (!seat.getTheaterId().equals(theaterId)) throw ApiException.missing("Seat not found in theater");
        requireEditableLayout(theaterId);
        seats.delete(seat);
    }

    public Movie createMovie(MovieInput input) { return movies.save(new Movie(input.title().trim(), input.durationMinutes())); }

    @Transactional public Movie updateMovie(Long id, MovieInput input) {
        Movie movie = movie(id);
        if (shows.existsByMovieId(id)) throw ApiException.conflict("Movie is used by a show");
        movie.setTitle(input.title().trim()); movie.setDurationMinutes(input.durationMinutes());
        return movie;
    }

    @Transactional public void deleteMovie(Long id) {
        movie(id);
        if (shows.existsByMovieId(id)) throw ApiException.conflict("Movie is used by a show");
        movies.deleteById(id);
    }

    public RefundPolicy createPolicy(PolicyInput input) {
        validatePolicy(input);
        return policies.save(new RefundPolicy(input.name().trim(), input.fullRefundHours(),
                input.partialRefundHours(), input.partialPercent()));
    }

    @Transactional public RefundPolicy updatePolicy(Long id, PolicyInput input) {
        validatePolicy(input);
        RefundPolicy policy = policy(id);
        policy.setName(input.name().trim()); policy.setFullRefundHours(input.fullRefundHours());
        policy.setPartialRefundHours(input.partialRefundHours()); policy.setPartialPercent(input.partialPercent());
        return policy;
    }

    @Transactional public void deletePolicy(Long id) {
        policy(id);
        if (shows.existsByRefundPolicyId(id)) throw ApiException.conflict("Refund policy is used by a show");
        policies.deleteById(id);
    }

    public DiscountCode createDiscount(DiscountInput input) {
        validateDiscount(input);
        return discounts.save(new DiscountCode(input.code().trim().toUpperCase(), input.percent(),
                input.validFrom(), input.validUntil(), input.active()));
    }

    @Transactional public DiscountCode updateDiscount(Long id, DiscountInput input) {
        validateDiscount(input);
        DiscountCode discount = discount(id);
        discount.setCode(input.code().trim().toUpperCase()); discount.setPercent(input.percent());
        discount.setValidFrom(input.validFrom()); discount.setValidUntil(input.validUntil()); discount.setActive(input.active());
        return discount;
    }

    public void deleteDiscount(Long id) { discount(id); discounts.deleteById(id); }

    @Transactional
    public MovieShow createShow(ShowInput input) {
        Theater theater = lockTheater(input.theaterId());
        lockCity(theater.getCityId());
        validateShow(input, null);
        MovieShow created = shows.save(new MovieShow(input.movieId(), input.theaterId(), input.refundPolicyId(),
                input.startsAt(), input.regularPrice(), input.premiumPrice(), input.weekendSurchargePercent()));
        showSeats.saveAll(seats.findByTheaterIdOrderById(input.theaterId()).stream()
                .map(seat -> new ShowSeat(created.getId(), seat.getId())).toList());
        return created;
    }

    @Transactional public MovieShow updateShow(Long id, ShowInput input) {
        MovieShow current = show(id);
        lockTheater(current.getTheaterId());
        MovieShow show = lockShow(id);
        if (bookings.existsByShowId(id)) throw ApiException.conflict("Show has bookings");
        if (!show.getTheaterId().equals(input.theaterId()))
            throw ApiException.conflict("Create a new show to use a different theater");
        validateShow(input, id);
        show.setMovieId(input.movieId()); show.setRefundPolicyId(input.refundPolicyId());
        show.setStartsAt(input.startsAt()); show.setRegularPrice(input.regularPrice());
        show.setPremiumPrice(input.premiumPrice()); show.setWeekendSurchargePercent(input.weekendSurchargePercent());
        return show;
    }

    @Transactional public void deleteShow(Long id) {
        MovieShow current = show(id);
        lockTheater(current.getTheaterId());
        lockShow(id);
        if (bookings.existsByShowId(id)) throw ApiException.conflict("Show has bookings");
        showSeats.deleteAll(showSeats.findByShowIdOrderBySeatId(id));
        shows.deleteById(id);
    }

    public List<City> allCities() { return cities.findAll(); }
    public List<Theater> allTheaters() { return theaters.findAll(); }
    public List<Theater> theatersInCity(Long cityId) { city(cityId); return theaters.findByCityIdOrderByName(cityId); }
    public List<Movie> allMovies() { return movies.findAll(); }
    public List<MovieShow> allShows() { return shows.findAll(); }
    public List<RefundPolicy> allPolicies() { return policies.findAll(); }
    public List<DiscountCode> allDiscounts() { return discounts.findAll(); }
    public List<TheaterSeat> theaterSeats(Long theaterId) { theater(theaterId); return seats.findByTheaterIdOrderById(theaterId); }

    public Page<MovieShow> browseShows(Long cityId, LocalDate date, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.bad("Invalid pagination");
        PageRequest pageable = PageRequest.of(page, size);
        Instant now = clock.instant();
        if (cityId == null) {
            if (date != null) throw ApiException.bad("cityId is required when filtering by local date");
            return shows.findByStartsAtAfterOrderByStartsAt(now, pageable);
        }
        City city = city(cityId);
        if (date == null) return shows.browseCity(cityId, now, pageable);
        ZoneId zone = ZoneId.of(city.getTimeZone());
        return shows.browseCityDate(cityId, date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant(), now, pageable);
    }

    public List<AvailabilityView> availability(Long showId) {
        MovieShow show = show(showId);
        Theater theater = theater(show.getTheaterId());
        ZoneId zone = ZoneId.of(city(theater.getCityId()).getTimeZone());
        Instant now = clock.instant();
        return showSeats.findByShowIdOrderBySeatId(showId).stream().map(row -> {
            TheaterSeat seat = seat(row.getSeatId());
            SeatStatus effective = row.getStatus() == SeatStatus.HELD && !row.getHoldExpiresAt().isAfter(now)
                    ? SeatStatus.AVAILABLE : row.getStatus();
            BigDecimal base = seat.getTier() == SeatTier.REGULAR ? show.getRegularPrice() : show.getPremiumPrice();
            return new AvailabilityView(seat.getId(), seat.getLabel(), seat.getTier(), effective,
                    PricingCalculator.seatPrice(base, show.getWeekendSurchargePercent(), show.getStartsAt(), zone));
        }).toList();
    }

    private void requireEditableLayout(Long theaterId) {
        if (shows.existsByTheaterId(theaterId)) throw ApiException.conflict("Seat layout is locked after show creation");
    }

    private void validatePolicy(PolicyInput input) {
        if (input.fullRefundHours() < input.partialRefundHours())
            throw ApiException.bad("Full-refund cutoff must be at least the partial-refund cutoff");
    }

    private void validateDiscount(DiscountInput input) {
        if (!input.validUntil().isAfter(input.validFrom())) throw ApiException.bad("Discount validity window is invalid");
    }

    private void validateShow(ShowInput input, Long updatingId) {
        Movie movie = movie(input.movieId());
        theater(input.theaterId());
        policy(input.refundPolicyId());
        if (!input.startsAt().isAfter(clock.instant())) throw ApiException.bad("Show must start in the future");
        if (seats.findByTheaterIdOrderById(input.theaterId()).isEmpty()) throw ApiException.bad("Theater has no seats");
        Instant end = input.startsAt().plusSeconds(movie.getDurationMinutes() * 60L);
        for (MovieShow existing : shows.findByTheaterId(input.theaterId())) {
            if (existing.getId().equals(updatingId)) continue;
            Instant existingEnd = existing.getStartsAt().plusSeconds(movie(existing.getMovieId()).getDurationMinutes() * 60L);
            if (input.startsAt().isBefore(existingEnd) && existing.getStartsAt().isBefore(end))
                throw ApiException.conflict("Show overlaps another show in this theater");
        }
    }
}
