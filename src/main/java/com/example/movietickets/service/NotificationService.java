package com.example.movietickets.service;

import com.example.movietickets.dto.ApiModels.NotificationView;
import com.example.movietickets.entity.Booking;
import com.example.movietickets.entity.BookingStatus;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.Notification;
import com.example.movietickets.entity.NotificationStatus;
import com.example.movietickets.entity.NotificationType;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.repository.MovieShowRepository;
import com.example.movietickets.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final BookingRepository bookings;
    private final MovieShowRepository shows;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, BookingRepository bookings,
                        MovieShowRepository shows, Clock clock) {
        this.notifications = notifications; this.bookings = bookings; this.shows = shows; this.clock = clock;
    }

    public void enqueue(Booking booking, NotificationType type) {
        if (!notifications.existsByBookingIdAndType(booking.getId(), type))
            notifications.save(new Notification(booking, type, clock.instant()));
    }

    @Transactional
    public void enqueueReminders() {
        Instant now = clock.instant();
        Instant horizon = now.plus(1, ChronoUnit.HOURS);
        for (Booking candidate : bookings.findUpcomingConfirmed(now, horizon)) {
            Booking booking = bookings.lockById(candidate.getId()).orElseThrow();
            if (booking.getStatus() != BookingStatus.CONFIRMED) continue;
            MovieShow show = shows.findById(booking.getShowId()).orElseThrow();
            if (show.getStartsAt().isAfter(now) && !show.getStartsAt().isAfter(horizon))
                enqueue(booking, NotificationType.REMINDER);
        }
    }

    @Transactional
    public void deliverPending() {
        Instant now = clock.instant();
        for (Notification notification : notifications.findByStatus(NotificationStatus.PENDING)) {
            if (notification.getType() == NotificationType.REMINDER) {
                Booking booking = bookings.lockById(notification.getBookingId()).orElseThrow();
                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                    notifications.delete(notification);
                    continue;
                }
            }
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setDeliveredAt(now);
        }
    }

    public Page<NotificationView> inbox(String username, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.bad("Invalid pagination");
        return notifications.findByUsernameAndStatusOrderByCreatedAtDesc(username,
                NotificationStatus.DELIVERED, PageRequest.of(page, size)).map(NotificationView::of);
    }
}
