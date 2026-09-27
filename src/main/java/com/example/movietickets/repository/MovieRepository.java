package com.example.movietickets.repository;

import com.example.movietickets.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {}
