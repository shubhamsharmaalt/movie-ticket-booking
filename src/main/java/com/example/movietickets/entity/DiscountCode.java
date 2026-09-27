package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "discount_codes")
public class DiscountCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal percent;
    @Column(nullable = false) private Instant validFrom;
    @Column(nullable = false) private Instant validUntil;
    @Column(nullable = false) private Boolean active;
    protected DiscountCode() {}
    public DiscountCode(String code, BigDecimal percent, Instant from, Instant until, boolean active) {
        this.code = code; this.percent = percent; this.validFrom = from; this.validUntil = until; this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code = value; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal value) { this.percent = value; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant value) { this.validFrom = value; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant value) { this.validUntil = value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active = value; }
}
