package com.gbm.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.MechanicProfile;

public interface MechanicProfileRepository extends JpaRepository<MechanicProfile, Long> {
    Optional<MechanicProfile> findByUserId(Long userId);
    List<MechanicProfile> findByVisibleTrue();
}
