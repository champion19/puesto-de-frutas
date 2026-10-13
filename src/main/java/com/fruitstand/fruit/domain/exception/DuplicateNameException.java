package com.fruitstand.fruit.domain.exception;

import com.fruitstand.fruit.domain.Fruit;

import lombok.Getter;

/**
 * Guarda el id y el estado de la fruta que ya usa el nombre: si está inactiva,
 * quien llama sabe cuál reactivar en vez de quedarse con un "ya existe" que no
 * aparece en el listado.
 */
@Getter
public class DuplicateNameException extends RuntimeException {

	private final Long existingId;
	private final boolean existingActive;

	public DuplicateNameException(Fruit existing) {
		super(existing.isActive()
				? "A fruit named " + existing.getName() + " already exists (id " + existing.getId() + ")"
				: "A fruit named " + existing.getName() + " already exists but is inactive (id "
						+ existing.getId() + "); reactivate it instead of creating it again");
		this.existingId = existing.getId();
		this.existingActive = existing.isActive();
	}

}
