package com.gbm.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.LiveLocation;

public interface LiveLocationRepository extends JpaRepository<LiveLocation, Long> {
    List<LiveLocation> findByBookingId(Long bookingId);
    Optional<LiveLocation> findByBookingIdAndUserId(Long bookingId, Long userId);
}
