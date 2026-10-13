package com.fruitstand.fruit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;
import com.fruitstand.fruit.domain.Unit;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaFruitRepository.class)
class JpaFruitRepositoryTest {

	@Autowired
	private FruitRepository repository;

	private Fruit save(String name, boolean active) {
		Fruit fruit = new Fruit(name, new BigDecimal("1000"), Unit.KG);
		if (!active) {
			fruit.deactivate();
		}
		return repository.save(fruit);
	}

	@Test
	void listsOnlyActiveFruitsOrderedByName() {
		save("Pear", true);
		save("Banana", false);
		save("Apple", true);

		assertThat(repository.findAllActive())
				.extracting(Fruit::getName)
				.containsExactly("Apple", "Pear");
	}

	@Test
	void doesNotFindDeactivatedFruitById() {
		Long active = save("Pear", true).getId();
		Long inactive = save("Banana", false).getId();

		assertThat(repository.findActiveById(active)).isPresent();
		assertThat(repository.findActiveById(inactive)).isEmpty();
	}

	@Test
	void detectsDuplicateNameIgnoringCase() {
		save("Apple", true);
		save("Banana", false);

		assertThat(repository.existsByName("APPLE")).isTrue();
		assertThat(repository.existsByName("banana")).isTrue();
		assertThat(repository.existsByName("Pear")).isFalse();
	}

	@Test
	void whenEditingIgnoresTheSameFruitsName() {
		Long apple = save("Apple", true).getId();
		Long pear = save("Pear", true).getId();

		assertThat(repository.existsByNameExcludingId("apple", apple)).isFalse();
		assertThat(repository.existsByNameExcludingId("apple", pear)).isTrue();
	}

}
