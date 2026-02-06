package com.gbm.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
}
