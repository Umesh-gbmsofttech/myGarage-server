package com.gbm.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.VehicleOwnerProfile;

public interface VehicleOwnerProfileRepository extends JpaRepository<VehicleOwnerProfile, Long> {
    Optional<VehicleOwnerProfile> findByUserId(Long userId);
}
