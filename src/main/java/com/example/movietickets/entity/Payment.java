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

@Entity @Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long bookingId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentOutcome outcome;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, unique = true, length = 80) private String reference;
    @Column(nullable = false) private Instant createdAt;
    protected Payment() {}
    public Payment(Long bookingId, PaymentOutcome outcome, BigDecimal amount, String reference, Instant at) {
        this.bookingId = bookingId; this.outcome = outcome; this.amount = amount;
        this.reference = reference; this.createdAt = at;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public PaymentOutcome getOutcome() { return outcome; }
    public void setOutcome(PaymentOutcome value) { this.outcome = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getReference() { return reference; }
    public void setReference(String value) { this.reference = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
}
