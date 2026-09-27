package com.example.movietickets.repository;

import com.example.movietickets.entity.Refund;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByBookingId(Long bookingId);
}
