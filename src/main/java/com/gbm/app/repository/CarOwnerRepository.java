package com.gbm.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.CarOwner;

public interface CarOwnerRepository extends JpaRepository<CarOwner,Long> {

}
