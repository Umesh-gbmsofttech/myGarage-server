package com.gbm.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gbm.app.dto.BookingSummaryDTO;
import com.gbm.app.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByOwnerId(Long ownerId);
    List<Booking> findByMechanicId(Long mechanicId);
    List<Booking> findByAssignedWorkerId(Long assignedWorkerId);
    Page<Booking> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);
    Page<Booking> findByMechanicIdOrderByUpdatedAtDesc(Long mechanicId, Pageable pageable);
    Page<Booking> findByAssignedWorkerIdOrderByUpdatedAtDesc(Long assignedWorkerId, Pageable pageable);

    @Query("""
        SELECT (COUNT(b) > 0)
        FROM Booking b
        WHERE b.id = :bookingId
          AND (b.owner.id = :userId OR b.mechanic.id = :userId OR (b.assignedWorker is not null AND b.assignedWorker.id = :userId))
    """)
    boolean isUserInBooking(@Param("bookingId") Long bookingId, @Param("userId") Long userId);

    @Query("""
        SELECT new com.gbm.app.dto.BookingSummaryDTO(
            b.id,
            b.status,
            b.vehicleMake,
            b.vehicleModel
        )
        FROM Booking b
        WHERE b.id = :id
    """)
    Optional<BookingSummaryDTO> findSummaryById(@Param("id") Long id);
}
