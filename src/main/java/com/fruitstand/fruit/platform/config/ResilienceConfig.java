package com.fruitstand.fruit.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Activa @Retryable: los reintentos de FruitService cuando dos operaciones chocan. */
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}
