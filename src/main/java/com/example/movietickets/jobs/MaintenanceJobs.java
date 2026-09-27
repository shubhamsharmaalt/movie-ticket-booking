package com.example.movietickets.jobs;

import com.example.movietickets.entity.Booking;
import com.example.movietickets.entity.BookingStatus;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.NotificationService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceJobs {
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final NotificationService notifications;
    private final Clock clock;

    public MaintenanceJobs(BookingRepository bookings, BookingService bookingService,
                    NotificationService notifications, Clock clock) {
        this.bookings = bookings; this.bookingService = bookingService;
        this.notifications = notifications; this.clock = clock;
    }

    @Scheduled(fixedDelay = 30000)
    void expireHolds() {
        for (Booking booking : bookings.findByStatusAndExpiresAtBefore(BookingStatus.HELD, clock.instant()))
            bookingService.expireOne(booking.getId());
    }

    @Scheduled(fixedDelay = 30000)
    void reminders() { notifications.enqueueReminders(); }

    @Scheduled(fixedDelay = 5000)
    void deliverNotifications() { notifications.deliverPending(); }
}
