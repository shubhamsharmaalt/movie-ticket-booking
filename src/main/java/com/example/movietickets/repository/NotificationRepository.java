package com.example.movietickets.repository;

import com.example.movietickets.entity.Notification;
import com.example.movietickets.entity.NotificationStatus;
import com.example.movietickets.entity.NotificationType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByBookingIdAndType(Long bookingId, NotificationType type);
    Page<Notification> findByUsernameAndStatusOrderByCreatedAtDesc(String username, NotificationStatus status, Pageable page);
    List<Notification> findByStatus(NotificationStatus status);
}
