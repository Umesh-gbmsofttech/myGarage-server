package com.gbm.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.AdminSettings;

public interface AdminSettingsRepository extends JpaRepository<AdminSettings, Long> {
}
