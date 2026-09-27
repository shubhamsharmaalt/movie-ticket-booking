package com.example.movietickets.repository;

import com.example.movietickets.entity.TheaterSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterSeatRepository extends JpaRepository<TheaterSeat, Long> {
    List<TheaterSeat> findByTheaterIdOrderById(Long theaterId);
    boolean existsByTheaterId(Long theaterId);
}
