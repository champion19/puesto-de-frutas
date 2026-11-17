package com.fruitstand.fruit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.Unit;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FruitPersistenceTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesAndReadsFruit() {
		Fruit fruit = new Fruit("Apple", new BigDecimal("2500.50"), Unit.KG);
		fruit.addStock(new BigDecimal("1.5"));

		Long id = entityManager.persistAndGetId(fruit, Long.class);
		entityManager.clear();

		Fruit saved = entityManager.find(Fruit.class, id);
		assertThat(saved.getName()).isEqualTo("Apple");
		assertThat(saved.getPrice()).isEqualByComparingTo("2500.50");
		assertThat(saved.getQuantity()).isEqualByComparingTo("1.5");
		assertThat(saved.getUnit()).isEqualTo(Unit.KG);
		assertThat(saved.isActive()).isTrue();
	}

}
