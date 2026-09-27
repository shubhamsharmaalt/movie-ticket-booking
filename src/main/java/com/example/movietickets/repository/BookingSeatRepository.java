package com.example.movietickets.repository;

import com.example.movietickets.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingIdOrderBySeatId(Long bookingId);
}
