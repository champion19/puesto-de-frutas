package com.fruitstand.fruit.platform.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera las consultas a partir del nombre de cada método. */
public interface SpringDataFruitRepository extends JpaRepository<FruitEntity, Long> {

	List<FruitEntity> findByActiveTrueOrderByNameAsc();

	List<FruitEntity> findAllByOrderByNameAsc();

	Optional<FruitEntity> findByIdAndActiveTrue(Long id);

	Optional<FruitEntity> findByNameIgnoreCase(String name);

}
