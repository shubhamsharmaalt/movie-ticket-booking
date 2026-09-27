package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity @Table(name = "booking_seats")
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long bookingId;
    @Column(nullable = false) private Long seatId;
    @Column(nullable = false, length = 20) private String label;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    protected BookingSeat() {}
    public BookingSeat(Long bookingId, Long seatId, String label, BigDecimal price) {
        this.bookingId = bookingId; this.seatId = seatId; this.label = label; this.price = price;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public Long getSeatId() { return seatId; }
    public void setSeatId(Long value) { this.seatId = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price = value; }
}
