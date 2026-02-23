package com.gbm.app.controller;

import com.gbm.app.dto.LiveLocationPointDTO;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/live-location")
@RequiredArgsConstructor
public class LiveLocationController {

    private final AuthService authService;
    private final LocationService locationService;

    @GetMapping("/{bookingId}")
    public ResponseEntity<List<LiveLocationPointDTO>> list(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookingId) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(locationService.getLiveLocationPoints(user, bookingId));
    }
}
