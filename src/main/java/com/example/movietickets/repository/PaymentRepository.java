package com.example.movietickets.repository;

import com.example.movietickets.entity.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
