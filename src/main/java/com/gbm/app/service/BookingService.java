package com.gbm.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gbm.app.dto.BookingRequest;
import com.gbm.app.dto.BookingRespondRequest;
import com.gbm.app.dto.BookingResponse;
import com.gbm.app.entity.Booking;
import com.gbm.app.entity.BookingStatus;
import com.gbm.app.entity.NotificationType;
import com.gbm.app.entity.User;
import com.gbm.app.repository.BookingRepository;
import com.gbm.app.repository.UserRepository;
import com.gbm.app.util.OtpUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BookingResponse createBooking(User owner, BookingRequest request) {
        User mechanic = userRepository.findById(request.getMechanicId())
            .orElseThrow(() -> new IllegalArgumentException("Mechanic not found"));

        Booking booking = new Booking();
        booking.setOwner(owner);
        booking.setMechanic(mechanic);
        booking.setStatus(BookingStatus.PENDING);
        booking.setVehicleMake(request.getVehicleMake());
        booking.setVehicleModel(request.getVehicleModel());
        booking.setVehicleYear(request.getVehicleYear());
        booking.setIssueDescription(request.getIssueDescription());

        Booking saved = bookingRepository.save(booking);

        notificationService.create(mechanic, "New booking request",
            "You have a new booking request from " + owner.getFirstName(),
            NotificationType.BOOKING_STATUS, "{\"bookingId\":" + saved.getId() + "}");

        return toResponse(saved);
    }

    public BookingResponse respondToBooking(User mechanic, Long bookingId, BookingRespondRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (request.getStatus() == BookingStatus.ACCEPTED) {
            booking.setStatus(BookingStatus.ACCEPTED);
            booking.setMeetOtp(OtpUtil.generateOtp());
            booking.setCompleteOtp(null);
        } else if (request.getStatus() == BookingStatus.DECLINED) {
            booking.setStatus(BookingStatus.DECLINED);
        } else {
            throw new IllegalArgumentException("Unsupported status");
        }

        Booking saved = bookingRepository.save(booking);

        notificationService.create(saved.getOwner(), "Booking update",
            "Your booking was " + saved.getStatus().name().toLowerCase(),
            NotificationType.BOOKING_STATUS, "{\"bookingId\":" + saved.getId() + "}");

        return toResponse(saved);
    }

    public List<BookingResponse> listForOwner(User owner) {
        return bookingRepository.findByOwnerId(owner.getId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<BookingResponse> listForMechanic(User mechanic) {
        return bookingRepository.findByMechanicId(mechanic.getId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public BookingResponse updateStatus(User user, Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getOwner().getId().equals(user.getId()) && !booking.getMechanic().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);
        notificationService.create(booking.getOwner(), "Booking update", "Booking status is now " + status.name().toLowerCase(),
            NotificationType.BOOKING_STATUS, "{\"bookingId\":" + saved.getId() + "}");
        notificationService.create(booking.getMechanic(), "Booking update", "Booking status is now " + status.name().toLowerCase(),
            NotificationType.BOOKING_STATUS, "{\"bookingId\":" + saved.getId() + "}");
        return toResponse(saved);
    }

    public BookingResponse verifyMeetOtp(User mechanic, Long bookingId, String code) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (booking.getMeetOtp() == null) {
            throw new IllegalArgumentException("Meet OTP not generated");
        }
        if (!code.equals(booking.getMeetOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        booking.setMeetVerified(true);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        Booking saved = bookingRepository.save(booking);
        notificationService.create(saved.getOwner(), "OTP verified", "OTP verification successful",
            NotificationType.OTP, "{\"bookingId\":" + saved.getId() + "}");
        notificationService.create(saved.getMechanic(), "OTP verified", "OTP verification successful",
            NotificationType.OTP, "{\"bookingId\":" + saved.getId() + "}");
        return toResponse(saved);
    }

    public BookingResponse verifyCompleteOtp(User mechanic, Long bookingId, String code) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (booking.getCompleteOtp() == null) {
            throw new IllegalArgumentException("Completion OTP not generated");
        }
        if (!code.equals(booking.getCompleteOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        booking.setCompleteVerified(true);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        notificationService.create(saved.getOwner(), "OTP verified", "OTP verification successful",
            NotificationType.OTP, "{\"bookingId\":" + saved.getId() + "}");
        notificationService.create(saved.getMechanic(), "OTP verified", "OTP verification successful",
            NotificationType.OTP, "{\"bookingId\":" + saved.getId() + "}");
        return toResponse(saved);
    }

    public BookingResponse generateCompleteOtp(User owner, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (!booking.isMeetVerified()) {
            throw new IllegalArgumentException("Meet OTP not verified");
        }
        if (booking.getCompleteOtp() == null || booking.getCompleteOtp().isBlank()) {
            booking.setCompleteOtp(OtpUtil.generateOtp());
        }
        Booking saved = bookingRepository.save(booking);
        notificationService.create(saved.getMechanic(), "Completion OTP ready", "Owner generated completion OTP",
            NotificationType.OTP, "{\"bookingId\":" + saved.getId() + "}");
        return toResponse(saved);
    }

    private BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setOwnerId(booking.getOwner().getId());
        response.setMechanicId(booking.getMechanic().getId());
        response.setStatus(booking.getStatus());
        response.setVehicleMake(booking.getVehicleMake());
        response.setVehicleModel(booking.getVehicleModel());
        response.setVehicleYear(booking.getVehicleYear());
        response.setIssueDescription(booking.getIssueDescription());
        response.setMeetOtp(booking.getMeetOtp());
        response.setCompleteOtp(booking.getCompleteOtp());
        response.setMeetVerified(booking.isMeetVerified());
        response.setCompleteVerified(booking.isCompleteVerified());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}
