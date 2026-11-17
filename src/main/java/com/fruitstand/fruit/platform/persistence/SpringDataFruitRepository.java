package com.fruitstand.fruit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fruitstand.fruit.domain.Fruit;

/** Spring Data genera las consultas a partir del nombre de cada método. */
public interface SpringDataFruitRepository extends JpaRepository<Fruit, Long> {

	List<Fruit> findByActiveTrueOrderByNameAsc();

	List<Fruit> findAllByOrderByNameAsc();

	Optional<Fruit> findByIdAndActiveTrue(Long id);

	Optional<Fruit> findByNameIgnoreCase(String name);

}
