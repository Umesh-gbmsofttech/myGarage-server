package com.gbm.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.BookingRequest;
import com.gbm.app.dto.BookingRespondRequest;
import com.gbm.app.dto.BookingResponse;
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

    @GetMapping("/owner")
    public ResponseEntity<List<BookingResponse>> ownerBookings(@RequestHeader("Authorization") String authorization) {
        User owner = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.listForOwner(owner));
    }

    @GetMapping("/mechanic")
    public ResponseEntity<List<BookingResponse>> mechanicBookings(@RequestHeader("Authorization") String authorization) {
        User mechanic = authService.requireUser(authorization);
        return ResponseEntity.ok(bookingService.listForMechanic(mechanic));
    }
}
