package com.gbm.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.service.MechanicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mechanics")
@RequiredArgsConstructor
public class MechanicController {

    private final MechanicService mechanicService;

    @GetMapping("/top-rated")
    public ResponseEntity<List<MechanicCardResponse>> topRated(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(mechanicService.topRated(limit));
    }

    @GetMapping("/random")
    public ResponseEntity<List<MechanicCardResponse>> random(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(mechanicService.random(limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MechanicCardResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "5") double radiusKm) {
        return ResponseEntity.ok(mechanicService.search(query, lat, lng, radiusKm));
    }
}
