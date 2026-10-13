package com.fruitstand.fruit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;
import com.fruitstand.fruit.domain.Unit;
import com.fruitstand.fruit.domain.exception.DuplicateNameException;
import com.fruitstand.fruit.domain.exception.FruitNotFoundException;

@ExtendWith(MockitoExtension.class)
class FruitServiceTest {

	@Mock
	private FruitRepository repository;

	@InjectMocks
	private FruitService service;

	private static BigDecimal n(String value) {
		return new BigDecimal(value);
	}

	private static Fruit apple() {
		return new Fruit("Apple", n("2500"), Unit.KG);
	}

	@Test
	void createsFruitWithNewName() {
		when(repository.existsByName("Apple")).thenReturn(false);
		when(repository.save(any(Fruit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Fruit created = service.create("  Apple ", n("2500"), Unit.KG);

		assertThat(created.getName()).isEqualTo("Apple");
	}

	@Test
	void doesNotCreateFruitWithDuplicateName() {
		when(repository.existsByName("Apple")).thenReturn(true);

		assertThatThrownBy(() -> service.create("Apple", n("2500"), Unit.KG))
				.isInstanceOf(DuplicateNameException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void findByIdFailsWhenMissingOrInactive() {
		when(repository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(1L))
				.isInstanceOf(FruitNotFoundException.class);
	}

	@Test
	void updatesFruitData() {
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple()));
		when(repository.existsByNameExcludingId("Red apple", 1L)).thenReturn(false);

		Fruit updated = service.update(1L, "Red apple", n("3000"), Unit.PIECE);

		assertThat(updated.getName()).isEqualTo("Red apple");
		assertThat(updated.getPrice()).isEqualByComparingTo("3000");
		assertThat(updated.getUnit()).isEqualTo(Unit.PIECE);
	}

	@Test
	void doesNotUpdateToAnotherFruitsName() {
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple()));
		when(repository.existsByNameExcludingId("Pear", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(1L, "Pear", n("3000"), Unit.KG))
				.isInstanceOf(DuplicateNameException.class);
	}

	@Test
	void movesFruitStock() {
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple()));

		service.addStock(1L, n("3"));
		Fruit fruit = service.removeStock(1L, n("1.5"));

		assertThat(fruit.getQuantity()).isEqualByComparingTo("1.5");
	}

	@Test
	void deactivatesAndReactivatesFruit() {
		Fruit fruit = apple();
		when(repository.findActiveById(1L)).thenReturn(Optional.of(fruit));
		when(repository.findById(1L)).thenReturn(Optional.of(fruit));

		service.deactivate(1L);
		assertThat(fruit.isActive()).isFalse();

		service.activate(1L);
		assertThat(fruit.isActive()).isTrue();
	}

}
