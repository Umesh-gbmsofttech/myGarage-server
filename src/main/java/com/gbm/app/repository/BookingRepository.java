package com.gbm.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByOwnerId(Long ownerId);
    List<Booking> findByMechanicId(Long mechanicId);
}
