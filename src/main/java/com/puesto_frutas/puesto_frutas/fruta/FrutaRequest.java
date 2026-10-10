package com.puesto_frutas.puesto_frutas.fruta;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar. Las reglas de precio y unidad las valida la entidad. */
public record FrutaRequest(
		@NotBlank @Size(max = 100) String nombre,
		@NotNull BigDecimal precio,
		@NotNull Unidad unidad) {
}
