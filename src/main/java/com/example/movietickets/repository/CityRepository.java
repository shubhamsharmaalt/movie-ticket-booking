package com.example.movietickets.repository;

import com.example.movietickets.entity.City;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CityRepository extends JpaRepository<City, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from City c where c.id = :id")
    Optional<City> lockById(@Param("id") Long id);
}
