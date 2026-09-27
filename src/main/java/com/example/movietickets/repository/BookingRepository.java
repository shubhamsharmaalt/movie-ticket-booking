package com.example.movietickets.repository;

import com.example.movietickets.entity.Booking;
import com.example.movietickets.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> lockById(@Param("id") Long id);
    Page<Booking> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant time);
    List<Booking> findByStatus(BookingStatus status);
    @Query("select b from Booking b, MovieShow s where b.showId = s.id and b.status = 'CONFIRMED' " +
            "and s.startsAt > :now and s.startsAt <= :horizon")
    List<Booking> findUpcomingConfirmed(@Param("now") Instant now, @Param("horizon") Instant horizon);
    boolean existsByShowId(Long showId);
}
