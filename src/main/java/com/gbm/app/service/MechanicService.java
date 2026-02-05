package com.gbm.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gbm.app.entity.Mechanic;
import com.gbm.app.repository.MechanicRepository;

@Service
public class MechanicService {

	@Autowired
	private MechanicRepository mechanicRepo;
	
	public Mechanic registerMechanic(Mechanic mechanic) {
		
		return mechanicRepo.save(mechanic);
	}
}
