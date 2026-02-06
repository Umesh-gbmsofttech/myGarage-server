package com.gbm.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.NotificationResponse;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@RequestHeader("Authorization") String authorization) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(notificationService.listForUser(user));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(notificationService.markRead(user, id));
    }
}
