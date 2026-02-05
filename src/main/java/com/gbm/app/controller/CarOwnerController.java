package com.gbm.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gbm.app.entity.CarOwner;
import com.gbm.app.service.CarOwnerService;

@RestController
@RequestMapping("api/carowner")
public class CarOwnerController {

	@Autowired
	private CarOwnerService cservice;
	
	@PostMapping("/signup")
	public ResponseEntity<CarOwner> signup(@RequestBody CarOwner carOwner){
		
		CarOwner registeredCarOwner = cservice.registerCarOwner(carOwner);
		return ResponseEntity.ok(registeredCarOwner);
	}
}
