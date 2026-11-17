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
	public List<Fruit> findAll() {
		return jpa.findAllByOrderByNameAsc();
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
	public Optional<Fruit> findByName(String name) {
		return jpa.findByNameIgnoreCase(name);
	}

	@Override
	public Fruit save(Fruit fruit) {
		return jpa.save(fruit);
	}

}
