package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity @Table(name = "refund_policies")
public class RefundPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(nullable = false) private Integer fullRefundHours;
    @Column(nullable = false) private Integer partialRefundHours;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal partialPercent;
    protected RefundPolicy() {}
    public RefundPolicy(String name, int full, int partial, BigDecimal percent) {
        this.name = name; this.fullRefundHours = full; this.partialRefundHours = partial; this.partialPercent = percent;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public Integer getFullRefundHours() { return fullRefundHours; }
    public void setFullRefundHours(Integer value) { this.fullRefundHours = value; }
    public Integer getPartialRefundHours() { return partialRefundHours; }
    public void setPartialRefundHours(Integer value) { this.partialRefundHours = value; }
    public BigDecimal getPartialPercent() { return partialPercent; }
    public void setPartialPercent(BigDecimal value) { this.partialPercent = value; }
}
