package com.gbm.app.controller;

import com.gbm.app.service.GoogleMapsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/maps")
public class MapsController {

    private final GoogleMapsService googleMapsService;

    public MapsController(GoogleMapsService googleMapsService) {
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocode(@RequestParam String address) {
        return ResponseEntity.ok(googleMapsService.geocode(address));
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, Object>> reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(googleMapsService.reverseGeocode(lat, lng));
    }

    @GetMapping("/directions")
    public ResponseEntity<Map<String, Object>> directions(
            @RequestParam String origin,
            @RequestParam String destination
    ) {
        return ResponseEntity.ok(googleMapsService.directions(origin, destination));
    }
}
