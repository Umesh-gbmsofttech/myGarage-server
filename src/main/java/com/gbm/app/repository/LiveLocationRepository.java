package com.gbm.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gbm.app.dto.LiveLocationDTO;
import com.gbm.app.dto.LiveLocationPointDTO;
import com.gbm.app.entity.LiveLocation;

public interface LiveLocationRepository extends JpaRepository<LiveLocation, Long> {
    List<LiveLocation> findByBookingId(Long bookingId);
    Optional<LiveLocation> findByBookingIdAndUserId(Long bookingId, Long userId);

    @Query("""
        SELECT new com.gbm.app.dto.LiveLocationPointDTO(
            l.user.id,
            l.latitude,
            l.longitude,
            l.updatedAt
        )
        FROM LiveLocation l
        WHERE l.booking.id = :bookingId
    """)
    List<LiveLocationPointDTO> findLocationPointsByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT new com.gbm.app.dto.LiveLocationDTO(
            l.booking.id,
            l.latitude,
            l.longitude,
            l.updatedAt
        )
        FROM LiveLocation l
        WHERE l.booking.id = :bookingId
    """)
    List<LiveLocationDTO> findDtosByBookingId(@Param("bookingId") Long bookingId);
}
