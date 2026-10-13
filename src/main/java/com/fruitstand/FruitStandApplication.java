package com.fruitstand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods // activa @Retryable (reintentos en FruitService)
public class FruitStandApplication {

	public static void main(String[] args) {
		SpringApplication.run(FruitStandApplication.class, args);
	}

}
