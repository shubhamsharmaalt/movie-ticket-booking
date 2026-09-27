package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "shows")
public class MovieShow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long movieId;
    @Column(nullable = false) private Long theaterId;
    @Column(nullable = false) private Long refundPolicyId;
    @Column(nullable = false) private Instant startsAt;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal regularPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal premiumPrice;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal weekendSurchargePercent;
    protected MovieShow() {}
    public MovieShow(Long movieId, Long theaterId, Long policyId, Instant startsAt,
              BigDecimal regularPrice, BigDecimal premiumPrice, BigDecimal weekendPercent) {
        this.movieId = movieId; this.theaterId = theaterId; this.refundPolicyId = policyId;
        this.startsAt = startsAt; this.regularPrice = regularPrice; this.premiumPrice = premiumPrice;
        this.weekendSurchargePercent = weekendPercent;
    }

    public Long getId() { return id; }
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long value) { this.movieId = value; }
    public Long getTheaterId() { return theaterId; }
    public void setTheaterId(Long value) { this.theaterId = value; }
    public Long getRefundPolicyId() { return refundPolicyId; }
    public void setRefundPolicyId(Long value) { this.refundPolicyId = value; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant value) { this.startsAt = value; }
    public BigDecimal getRegularPrice() { return regularPrice; }
    public void setRegularPrice(BigDecimal value) { this.regularPrice = value; }
    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public void setPremiumPrice(BigDecimal value) { this.premiumPrice = value; }
    public BigDecimal getWeekendSurchargePercent() { return weekendSurchargePercent; }
    public void setWeekendSurchargePercent(BigDecimal value) { this.weekendSurchargePercent = value; }
}
