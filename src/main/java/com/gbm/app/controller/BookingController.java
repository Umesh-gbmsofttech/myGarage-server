package com.gbm.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.BookingRequest;
import com.gbm.app.dto.BookingReportRequest;
import com.gbm.app.dto.BookingRespondRequest;
import com.gbm.app.dto.BookingResponse;
import com.gbm.app.dto.BookingSummaryDTO;
import com.gbm.app.dto.OtpCodeRequest;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final AuthService authService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@RequestHeader("Authorization") String authorization,
            @RequestBody BookingRequest request) {
        User owner = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.createBooking(owner, request));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<BookingResponse> respond(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody BookingRespondRequest request) {
        User mechanic = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.respondToBooking(mechanic, id, request));
    }

    @PostMapping("/{id}/verify-meet-otp")
    public ResponseEntity<BookingResponse> verifyMeetOtp(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody OtpCodeRequest request) {
        User mechanic = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.verifyMeetOtp(mechanic, id, request.getCode()));
    }

    @PostMapping("/{id}/verify-complete-otp")
    public ResponseEntity<BookingResponse> verifyCompleteOtp(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody OtpCodeRequest request) {
        User mechanic = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.verifyCompleteOtp(mechanic, id, request.getCode()));
    }

    @PostMapping("/{id}/generate-complete-otp")
    public ResponseEntity<BookingResponse> generateCompleteOtp(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User owner = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.generateCompleteOtp(owner, id));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<BookingResponse> submitReport(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody BookingReportRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.submitReport(user, id, request.getDescription()));
    }

    @GetMapping("/owner")
    public ResponseEntity<Page<BookingResponse>> ownerBookings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User owner = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.listForOwner(owner, page, size));
    }

    @GetMapping("/mechanic")
    public ResponseEntity<Page<BookingResponse>> mechanicBookings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User mechanic = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.listForMechanic(mechanic, page, size));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<BookingSummaryDTO> bookingSummary(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.getBookingSummary(user, id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> booking(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.getBooking(user, id));
    }
}
