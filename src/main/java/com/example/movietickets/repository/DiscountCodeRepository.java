package com.example.movietickets.repository;

import com.example.movietickets.entity.DiscountCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);
}
