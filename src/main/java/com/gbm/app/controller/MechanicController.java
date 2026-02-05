package com.gbm.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.entity.Mechanic;
import com.gbm.app.service.MechanicService;

@RestController
@RequestMapping("/api/mechanic")
public class MechanicController {

	@Autowired
	private MechanicService mservice;
	
	@PostMapping("/signup")
	public ResponseEntity<Mechanic> signup(@RequestBody Mechanic mechanic) {
		Mechanic registeredMechanic =mservice.registerMechanic(mechanic);
		
		return ResponseEntity.ok(registeredMechanic);
	}
}
