package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "theater_seats")
public class TheaterSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long theaterId;
    @Column(nullable = false, length = 20) private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SeatTier tier;
    protected TheaterSeat() {}
    public TheaterSeat(Long theaterId, String label, SeatTier tier) {
        this.theaterId = theaterId; this.label = label; this.tier = tier;
    }

    public Long getId() { return id; }
    public Long getTheaterId() { return theaterId; }
    public void setTheaterId(Long value) { this.theaterId = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label = value; }
    public SeatTier getTier() { return tier; }
    public void setTier(SeatTier value) { this.tier = value; }
}
