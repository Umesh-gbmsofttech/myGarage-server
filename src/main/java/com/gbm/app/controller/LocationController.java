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

import com.gbm.app.dto.LocationUpdateRequest;
import com.gbm.app.entity.LiveLocation;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.LocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final AuthService authService;
    private final LocationService locationService;

    @PostMapping("/bookings/{id}")
    public ResponseEntity<LiveLocation> update(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id, @RequestBody LocationUpdateRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(locationService.updateLocation(user, id, request));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<List<LiveLocation>> list(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(locationService.getLocations(user, id));
    }
}
