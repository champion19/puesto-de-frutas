package com.fruitstand.fruit.domain;

import java.util.List;
import java.util.Optional;

/**
 * Puerto: lo que el dominio necesita para guardar y buscar frutas. No sabe
 * nada de JPA; la implementación vive en infrastructure.persistence.
 */
public interface FruitRepository {

	/** Catálogo: solo las frutas que se siguen ofreciendo, ordenadas por nombre. */
	List<Fruit> findAllActive();

	/** Todas, incluidas las desactivadas, ordenadas por nombre. */
	List<Fruit> findAll();

	Optional<Fruit> findActiveById(Long id);

	/** Incluye las desactivadas (p. ej. para reactivarlas). */
	Optional<Fruit> findById(Long id);

	/**
	 * Sin distinguir mayúsculas. Incluye las desactivadas: el nombre sigue
	 * ocupado en la tabla (uk_fruit_name).
	 */
	Optional<Fruit> findByName(String name);

	Fruit save(Fruit fruit);

}
