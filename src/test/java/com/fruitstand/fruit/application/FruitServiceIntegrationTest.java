package com.fruitstand.fruit.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.Unit;
import com.fruitstand.fruit.domain.exception.DuplicateNameException;
import com.fruitstand.fruit.infrastructure.persistence.JpaFruitRepository;
import com.fruitstand.fruit.infrastructure.persistence.SpringDataFruitRepository;

/** Service con la base real (H2): cubre lo que el mock no ve, como el flush de Hibernate. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ FruitService.class, JpaFruitRepository.class })
class FruitServiceIntegrationTest {

	@Autowired
	private FruitService service;

	/** Spring Data directo: el puerto no tiene saveAndFlush. */
	@Autowired
	private SpringDataFruitRepository repository;

	@Test
	void renamingToAnotherFruitsNameThrowsDuplicateName() {
		repository.saveAndFlush(new Fruit("Apple", new BigDecimal("1000"), Unit.KG));
		Long pear = repository.saveAndFlush(new Fruit("Pear", new BigDecimal("1000"), Unit.KG)).getId();

		assertThatThrownBy(() -> service.update(pear, " Apple ", new BigDecimal("1000"), Unit.KG))
				.isInstanceOf(DuplicateNameException.class);
	}

}
