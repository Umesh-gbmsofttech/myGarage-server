package com.gbm.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.UpdateProfileRequest;
import com.gbm.app.dto.UserProfileResponse;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.ProfileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@RequestHeader("Authorization") String authorization) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(profileService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> update(@RequestHeader("Authorization") String authorization,
            @RequestBody UpdateProfileRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(profileService.updateProfile(user, request));
    }
}
