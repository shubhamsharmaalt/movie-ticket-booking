package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long bookingId;
    @Column(nullable = false, length = 80) private String username;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationStatus status;
    @Column(nullable = false) private Instant createdAt;
    private Instant deliveredAt;
    protected Notification() {}
    public Notification(Booking booking, NotificationType type, Instant at) {
        this.bookingId = booking.getId(); this.username = booking.getUsername(); this.type = type;
        this.status = NotificationStatus.PENDING; this.createdAt = at;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType value) { this.type = value; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus value) { this.status = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant value) { this.deliveredAt = value; }
}
