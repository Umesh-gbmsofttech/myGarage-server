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

import com.gbm.app.dto.ReviewRequest;
import com.gbm.app.entity.Review;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final AuthService authService;
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> create(@RequestHeader("Authorization") String authorization,
            @RequestBody ReviewRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(reviewService.create(user, request));
    }

    @GetMapping("/platform")
    public ResponseEntity<List<Review>> platformReviews() {
        return ResponseEntity.ok(reviewService.listPlatformReviews());
    }

    @GetMapping("/mechanics/{id}")
    public ResponseEntity<List<Review>> mechanicReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.listMechanicReviews(id));
    }
}
