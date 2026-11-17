package com.fruitstand.fruit.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fruitstand.fruit.domain.Unit;

/** Datos para crear o editar. Las reglas de precio y unidad las valida la entidad. */
public record FruitRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull BigDecimal price,
		@NotNull Unit unit) {
}
