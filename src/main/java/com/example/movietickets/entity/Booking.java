package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long showId;
    @Column(nullable = false, length = 80) private String username;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingStatus status;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(length = 40) private String discountCode;
    @Column(nullable = false) private Integer fullRefundHours;
    @Column(nullable = false) private Integer partialRefundHours;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal partialPercent;
    @Column(nullable = false) private Instant createdAt;
    private Instant confirmedAt;
    protected Booking() {}
    public Booking(Long showId, String username, Instant expiresAt, BigDecimal total, String discountCode,
            RefundPolicy policy, Instant createdAt) {
        this.showId = showId; this.username = username; this.status = BookingStatus.HELD;
        this.expiresAt = expiresAt; this.total = total; this.discountCode = discountCode;
        this.fullRefundHours = policy.getFullRefundHours(); this.partialRefundHours = policy.getPartialRefundHours();
        this.partialPercent = policy.getPartialPercent(); this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getShowId() { return showId; }
    public void setShowId(Long value) { this.showId = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus value) { this.status = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal value) { this.total = value; }
    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String value) { this.discountCode = value; }
    public Integer getFullRefundHours() { return fullRefundHours; }
    public void setFullRefundHours(Integer value) { this.fullRefundHours = value; }
    public Integer getPartialRefundHours() { return partialRefundHours; }
    public void setPartialRefundHours(Integer value) { this.partialRefundHours = value; }
    public BigDecimal getPartialPercent() { return partialPercent; }
    public void setPartialPercent(BigDecimal value) { this.partialPercent = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }
}
