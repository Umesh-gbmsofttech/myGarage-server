package com.gbm.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.gbm.app.dto.AuthRequest;
import com.gbm.app.dto.AuthResponse;
import com.gbm.app.dto.ForgotPasswordRequest;
import com.gbm.app.dto.ResetPasswordRequest;
import com.gbm.app.dto.SignupMechanicRequest;
import com.gbm.app.dto.SignupOwnerRequest;
import com.gbm.app.dto.VerifyOtpRequest;
import com.gbm.app.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/mechanic")
    public ResponseEntity<AuthResponse> signupMechanic(@Valid @RequestBody SignupMechanicRequest request) {
        return ResponseEntity.ok(authService.signupMechanic(request));
    }

    @PostMapping("/signup/owner")
    public ResponseEntity<AuthResponse> signupOwner(@Valid @RequestBody SignupOwnerRequest request) {
        return ResponseEntity.ok(authService.signupOwner(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.sendForgotPasswordOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
