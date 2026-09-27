package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false) private Integer durationMinutes;
    protected Movie() {}
    public Movie(String title, Integer durationMinutes) { this.title = title; this.durationMinutes = durationMinutes; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer value) { this.durationMinutes = value; }
}
