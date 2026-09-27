package com.example.movietickets.repository;

import com.example.movietickets.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    List<ShowSeat> findByShowIdOrderBySeatId(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.showId = :showId and s.seatId = :seatId")
    Optional<ShowSeat> lockOne(@Param("showId") Long showId, @Param("seatId") Long seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.bookingId = :bookingId order by s.seatId")
    List<ShowSeat> lockByBooking(@Param("bookingId") Long bookingId);
}
