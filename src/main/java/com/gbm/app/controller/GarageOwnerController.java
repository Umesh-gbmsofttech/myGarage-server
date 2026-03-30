package com.gbm.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.dto.GarageOwnerRegistrationRequest;
import com.gbm.app.dto.GarageWorkerRequest;
import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.dto.UserProfileResponse;
import com.gbm.app.entity.User;
import com.gbm.app.service.AuthService;
import com.gbm.app.service.GarageOwnerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/garage-owner")
@RequiredArgsConstructor
public class GarageOwnerController {

    private final AuthService authService;
    private final GarageOwnerService garageOwnerService;

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestHeader("Authorization") String authorization,
            @RequestBody GarageOwnerRegistrationRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(garageOwnerService.registerAsGarageOwner(user, request));
    }

    @GetMapping("/mechanics")
    public ResponseEntity<List<MechanicCardResponse>> myMechanics(@RequestHeader("Authorization") String authorization) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(garageOwnerService.listMyMechanics(user));
    }

    @PostMapping("/mechanics")
    public ResponseEntity<MechanicCardResponse> addMechanic(@RequestHeader("Authorization") String authorization,
            @RequestBody GarageWorkerRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(garageOwnerService.addWorker(user, request));
    }

    @PutMapping("/mechanics/{workerUserId}")
    public ResponseEntity<MechanicCardResponse> updateMechanic(@RequestHeader("Authorization") String authorization,
            @PathVariable Long workerUserId, @RequestBody GarageWorkerRequest request) {
        User user = authService.requireUser(authorization);
        return ResponseEntity.ok(garageOwnerService.updateWorker(user, workerUserId, request));
    }

    @DeleteMapping("/mechanics/{workerUserId}")
    public ResponseEntity<Void> deleteMechanic(@RequestHeader("Authorization") String authorization,
            @PathVariable Long workerUserId) {
        User user = authService.requireUser(authorization);
        garageOwnerService.deleteWorker(user, workerUserId);
        return ResponseEntity.noContent().build();
    }
}
