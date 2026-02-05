package com.gbm.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gbm.app.entity.CarOwner;
import com.gbm.app.repository.CarOwnerRepository;

@Service
public class CarOwnerService {
	
	@Autowired
	private CarOwnerRepository carOwnerRepo;
	
	public CarOwner registerCarOwner(CarOwner carOwner) {
		
		return carOwnerRepo.save(carOwner);
	}

}
