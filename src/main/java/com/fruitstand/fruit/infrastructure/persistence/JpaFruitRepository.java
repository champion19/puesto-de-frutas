package com.fruitstand.fruit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;

import lombok.RequiredArgsConstructor;

/** Adaptador: implementa el puerto del dominio delegando en Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class JpaFruitRepository implements FruitRepository {

	private final SpringDataFruitRepository jpa;

	@Override
	public List<Fruit> findAllActive() {
		return jpa.findByActiveTrueOrderByNameAsc();
	}

	@Override
	public Optional<Fruit> findActiveById(Long id) {
		return jpa.findByIdAndActiveTrue(id);
	}

	@Override
	public Optional<Fruit> findById(Long id) {
		return jpa.findById(id);
	}

	@Override
	public boolean existsByName(String name) {
		return jpa.existsByNameIgnoreCase(name);
	}

	@Override
	public boolean existsByNameExcludingId(String name, Long id) {
		return jpa.existsByNameIgnoreCaseAndIdNot(name, id);
	}

	@Override
	public Fruit save(Fruit fruit) {
		return jpa.save(fruit);
	}

}
