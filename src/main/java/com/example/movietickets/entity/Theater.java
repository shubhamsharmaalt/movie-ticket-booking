package com.example.movietickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "theaters")
public class Theater {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long cityId;
    @Column(nullable = false, length = 120) private String name;
    protected Theater() {}
    public Theater(Long cityId, String name) { this.cityId = cityId; this.name = name; }

    public Long getId() { return id; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long value) { this.cityId = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
}
