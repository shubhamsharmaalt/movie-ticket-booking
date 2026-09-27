package com.example.movietickets.repository;

import com.example.movietickets.entity.Theater;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
    boolean existsByCityId(Long cityId);
    List<Theater> findByCityIdOrderByName(Long cityId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Theater t where t.id = :id")
    Optional<Theater> lockById(@Param("id") Long id);
}
