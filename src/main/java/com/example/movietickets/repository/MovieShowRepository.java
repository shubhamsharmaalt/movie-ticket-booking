package com.example.movietickets.repository;

import com.example.movietickets.entity.MovieShow;
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

public interface MovieShowRepository extends JpaRepository<MovieShow, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MovieShow s where s.id = :id")
    Optional<MovieShow> lockById(@Param("id") Long id);
    List<MovieShow> findByTheaterId(Long theaterId);
    boolean existsByTheaterId(Long theaterId);
    boolean existsByMovieId(Long movieId);
    boolean existsByRefundPolicyId(Long policyId);

    Page<MovieShow> findByStartsAtAfterOrderByStartsAt(Instant now, Pageable page);

    @Query("select s from MovieShow s, Theater t where s.theaterId = t.id and t.cityId = :cityId " +
            "and s.startsAt > :now order by s.startsAt")
    Page<MovieShow> browseCity(@Param("cityId") Long cityId, @Param("now") Instant now, Pageable page);

    @Query("select s from MovieShow s, Theater t where s.theaterId = t.id and t.cityId = :cityId " +
            "and s.startsAt >= :from and s.startsAt < :until and s.startsAt > :now order by s.startsAt")
    Page<MovieShow> browseCityDate(@Param("cityId") Long cityId, @Param("from") Instant from,
                                   @Param("until") Instant until, @Param("now") Instant now, Pageable page);
}
