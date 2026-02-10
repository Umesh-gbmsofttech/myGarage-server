package com.gbm.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyGarageAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyGarageAppApplication.class, args);
	}

}
