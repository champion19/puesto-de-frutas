package com.puesto_frutas.puesto_frutas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods // activa @Retryable (reintentos en FrutaService)
public class PuestoFrutasApplication {

	public static void main(String[] args) {
		SpringApplication.run(PuestoFrutasApplication.class, args);
	}

}
