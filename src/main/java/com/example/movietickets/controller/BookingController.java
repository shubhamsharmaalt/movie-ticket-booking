package com.example.movietickets.controller;

import com.example.movietickets.dto.ApiModels.BookingView;
import com.example.movietickets.dto.ApiModels.HoldInput;
import com.example.movietickets.dto.ApiModels.NotificationView;
import com.example.movietickets.dto.ApiModels.PageView;
import com.example.movietickets.dto.ApiModels.PaymentInput;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.NotificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookings;
    private final NotificationService notifications;
    BookingController(BookingService bookings, NotificationService notifications) {
        this.bookings = bookings; this.notifications = notifications;
    }

    @PostMapping("/bookings/holds")
    ResponseEntity<BookingView> hold(@Valid @RequestBody HoldInput input, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookings.hold(input, principal.getName()));
    }

    @PostMapping("/bookings/{id}/payments")
    BookingView pay(@PathVariable Long id, @Valid @RequestBody PaymentInput input, Principal principal) {
        return bookings.pay(id, principal.getName(), input.outcome());
    }

    @PostMapping("/bookings/{id}/cancellation")
    BookingView cancel(@PathVariable Long id, Principal principal) { return bookings.cancel(id, principal.getName()); }

    @GetMapping("/bookings/{id}")
    BookingView get(@PathVariable Long id, Principal principal) { return bookings.get(id, principal.getName()); }

    @GetMapping("/bookings")
    PageView<BookingView> history(Principal principal, @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return PageView.of(bookings.history(principal.getName(), page, size));
    }

    @GetMapping("/notifications")
    PageView<NotificationView> inbox(Principal principal, @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return PageView.of(notifications.inbox(principal.getName(), page, size));
    }
}
