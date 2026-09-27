package com.example.movietickets.controller;

import com.example.movietickets.dto.ApiModels.AvailabilityView;
import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.CityView;
import com.example.movietickets.dto.ApiModels.DiscountInput;
import com.example.movietickets.dto.ApiModels.DiscountView;
import com.example.movietickets.dto.ApiModels.MovieInput;
import com.example.movietickets.dto.ApiModels.MovieView;
import com.example.movietickets.dto.ApiModels.PageView;
import com.example.movietickets.dto.ApiModels.PolicyInput;
import com.example.movietickets.dto.ApiModels.PolicyView;
import com.example.movietickets.dto.ApiModels.SeatInput;
import com.example.movietickets.dto.ApiModels.SeatView;
import com.example.movietickets.dto.ApiModels.ShowInput;
import com.example.movietickets.dto.ApiModels.ShowView;
import com.example.movietickets.dto.ApiModels.TheaterInput;
import com.example.movietickets.dto.ApiModels.TheaterView;
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.service.CatalogService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService service;
    CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/cities") List<CityView> cities() { return service.allCities().stream().map(CityView::of).toList(); }
    @GetMapping("/cities/{id}") CityView city(@PathVariable Long id) { return CityView.of(service.city(id)); }
    @GetMapping("/theaters") List<TheaterView> theaters(@RequestParam Long cityId) {
        return service.theatersInCity(cityId).stream().map(TheaterView::of).toList();
    }
    @GetMapping("/theaters/{id}") TheaterView theater(@PathVariable Long id) {
        return TheaterView.of(service.theater(id));
    }
    @GetMapping("/movies") List<MovieView> movies() { return service.allMovies().stream().map(MovieView::of).toList(); }
    @GetMapping("/movies/{id}") MovieView movie(@PathVariable Long id) { return MovieView.of(service.movie(id)); }
    @GetMapping("/shows") PageView<ShowView> shows(@RequestParam(required = false) Long cityId,
            @RequestParam(required = false) LocalDate date, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageView.of(service.browseShows(cityId, date, page, size).map(ShowView::of));
    }
    @GetMapping("/shows/{id}") ShowView show(@PathVariable Long id) { return ShowView.of(service.show(id)); }
    @GetMapping("/shows/{id}/seats") List<AvailabilityView> availability(@PathVariable Long id) {
        return service.availability(id);
    }

    @PostMapping("/admin/cities") ResponseEntity<CityView> addCity(@Valid @RequestBody CityInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CityView.of(service.createCity(input)));
    }
    @GetMapping("/admin/cities") List<CityView> adminCities() { return cities(); }
    @GetMapping("/admin/cities/{id}") CityView adminCity(@PathVariable Long id) { return CityView.of(service.city(id)); }
    @PutMapping("/admin/cities/{id}") CityView updateCity(@PathVariable Long id, @Valid @RequestBody CityInput input) {
        return CityView.of(service.updateCity(id, input));
    }
    @DeleteMapping("/admin/cities/{id}") ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        service.deleteCity(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/theaters") ResponseEntity<TheaterView> addTheater(@Valid @RequestBody TheaterInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TheaterView.of(service.createTheater(input)));
    }
    @GetMapping("/admin/theaters") List<TheaterView> adminTheaters() {
        return service.allTheaters().stream().map(TheaterView::of).toList();
    }
    @GetMapping("/admin/theaters/{id}") TheaterView adminTheater(@PathVariable Long id) {
        return TheaterView.of(service.theater(id));
    }
    @PutMapping("/admin/theaters/{id}") TheaterView updateTheater(@PathVariable Long id,
            @Valid @RequestBody TheaterInput input) { return TheaterView.of(service.updateTheater(id, input)); }
    @DeleteMapping("/admin/theaters/{id}") ResponseEntity<Void> deleteTheater(@PathVariable Long id) {
        service.deleteTheater(id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/admin/theaters/{id}/seats") ResponseEntity<SeatView> addSeat(@PathVariable Long id,
            @Valid @RequestBody SeatInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SeatView.of(service.createSeat(id, input)));
    }
    @GetMapping("/admin/theaters/{id}/seats") List<SeatView> seats(@PathVariable Long id) {
        return service.theaterSeats(id).stream().map(SeatView::of).toList();
    }
    @GetMapping("/admin/theaters/{id}/seats/{seatId}") SeatView seat(@PathVariable Long id, @PathVariable Long seatId) {
        TheaterSeat seat = service.seat(seatId);
        if (!seat.getTheaterId().equals(id)) throw ApiException.missing("Seat not found in theater");
        return SeatView.of(seat);
    }
    @PutMapping("/admin/theaters/{id}/seats/{seatId}") SeatView updateSeat(@PathVariable Long id,
            @PathVariable Long seatId, @Valid @RequestBody SeatInput input) {
        return SeatView.of(service.updateSeat(id, seatId, input));
    }
    @DeleteMapping("/admin/theaters/{id}/seats/{seatId}") ResponseEntity<Void> deleteSeat(@PathVariable Long id,
            @PathVariable Long seatId) { service.deleteSeat(id, seatId); return ResponseEntity.noContent().build(); }

    @PostMapping("/admin/movies") ResponseEntity<MovieView> addMovie(@Valid @RequestBody MovieInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(MovieView.of(service.createMovie(input)));
    }
    @GetMapping("/admin/movies") List<MovieView> adminMovies() { return movies(); }
    @GetMapping("/admin/movies/{id}") MovieView adminMovie(@PathVariable Long id) { return MovieView.of(service.movie(id)); }
    @PutMapping("/admin/movies/{id}") MovieView updateMovie(@PathVariable Long id, @Valid @RequestBody MovieInput input) {
        return MovieView.of(service.updateMovie(id, input));
    }
    @DeleteMapping("/admin/movies/{id}") ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        service.deleteMovie(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/refund-policies") ResponseEntity<PolicyView> addPolicy(@Valid @RequestBody PolicyInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyView.of(service.createPolicy(input)));
    }
    @GetMapping("/admin/refund-policies") List<PolicyView> policies() {
        return service.allPolicies().stream().map(PolicyView::of).toList();
    }
    @GetMapping("/admin/refund-policies/{id}") PolicyView policy(@PathVariable Long id) {
        return PolicyView.of(service.policy(id));
    }
    @PutMapping("/admin/refund-policies/{id}") PolicyView updatePolicy(@PathVariable Long id,
            @Valid @RequestBody PolicyInput input) { return PolicyView.of(service.updatePolicy(id, input)); }
    @DeleteMapping("/admin/refund-policies/{id}") ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        service.deletePolicy(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/discount-codes") ResponseEntity<DiscountView> addDiscount(@Valid @RequestBody DiscountInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountView.of(service.createDiscount(input)));
    }
    @GetMapping("/admin/discount-codes") List<DiscountView> discounts() {
        return service.allDiscounts().stream().map(DiscountView::of).toList();
    }
    @GetMapping("/admin/discount-codes/{id}") DiscountView discount(@PathVariable Long id) {
        return DiscountView.of(service.discount(id));
    }
    @PutMapping("/admin/discount-codes/{id}") DiscountView updateDiscount(@PathVariable Long id,
            @Valid @RequestBody DiscountInput input) { return DiscountView.of(service.updateDiscount(id, input)); }
    @DeleteMapping("/admin/discount-codes/{id}") ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        service.deleteDiscount(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/shows") ResponseEntity<ShowView> addShow(@Valid @RequestBody ShowInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ShowView.of(service.createShow(input)));
    }
    @GetMapping("/admin/shows") List<ShowView> adminShows() {
        return service.allShows().stream().map(ShowView::of).toList();
    }
    @GetMapping("/admin/shows/{id}") ShowView adminShow(@PathVariable Long id) {
        return ShowView.of(service.show(id));
    }
    @PutMapping("/admin/shows/{id}") ShowView updateShow(@PathVariable Long id, @Valid @RequestBody ShowInput input) {
        return ShowView.of(service.updateShow(id, input));
    }
    @DeleteMapping("/admin/shows/{id}") ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        service.deleteShow(id); return ResponseEntity.noContent().build();
    }
}
