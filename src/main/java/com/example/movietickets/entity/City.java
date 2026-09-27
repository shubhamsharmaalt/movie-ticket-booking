package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "cities")
public class City {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(nullable = false, length = 80) private String timeZone;
    protected City() {}
    public City(String name, String timeZone) { this.name = name; this.timeZone = timeZone; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String value) { this.timeZone = value; }
}
