package com.fruitstand.fruit.platform.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Adaptador: implementa el puerto del dominio delegando en Spring Data JPA.
 * Afuera solo salen objetos Fruit; FruitEntity no sale de este paquete.
 */
@Repository
@RequiredArgsConstructor
public class JpaFruitRepository implements FruitRepository {

	private final SpringDataFruitRepository jpa;

	@Override
	public List<Fruit> findAllActive() {
		return jpa.findByActiveTrueOrderByNameAsc().stream().map(FruitEntity::toDomain).toList();
	}

	@Override
	public List<Fruit> findAll() {
		return jpa.findAllByOrderByNameAsc().stream().map(FruitEntity::toDomain).toList();
	}

	@Override
	public Optional<Fruit> findActiveById(Long id) {
		return jpa.findByIdAndActiveTrue(id).map(FruitEntity::toDomain);
	}

	@Override
	public Optional<Fruit> findById(Long id) {
		return jpa.findById(id).map(FruitEntity::toDomain);
	}

	@Override
	public Optional<Fruit> findByName(String name) {
		return jpa.findByNameIgnoreCase(name).map(FruitEntity::toDomain);
	}

	/**
	 * Nueva (sin id) → INSERT. Existente → UPDATE solo si la versión que trae
	 * sigue siendo la de la base; si otra operación la cambió, falla con
	 * OptimisticLockingFailureException en vez de pisar ese cambio.
	 */
	@Override
	public Fruit save(Fruit fruit) {
		return jpa.save(FruitEntity.from(fruit)).toDomain();
	}

}
