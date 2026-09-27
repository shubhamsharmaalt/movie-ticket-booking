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

@Entity @Table(name = "show_seats")
public class ShowSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long showId;
    @Column(nullable = false) private Long seatId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SeatStatus status = SeatStatus.AVAILABLE;
    private Long bookingId;
    private Instant holdExpiresAt;
    protected ShowSeat() {}
    public ShowSeat(Long showId, Long seatId) { this.showId = showId; this.seatId = seatId; }

    public Long getId() { return id; }
    public Long getShowId() { return showId; }
    public void setShowId(Long value) { this.showId = value; }
    public Long getSeatId() { return seatId; }
    public void setSeatId(Long value) { this.seatId = value; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus value) { this.status = value; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(Instant value) { this.holdExpiresAt = value; }
}
