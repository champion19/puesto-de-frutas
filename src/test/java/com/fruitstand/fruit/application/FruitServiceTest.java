package com.fruitstand.fruit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

	/** El id lo pone la base al guardar; en un test unitario se asigna a mano. */
	private static Fruit fruitWithId(Long id, String name) {
		Fruit fruit = new Fruit(name, n("2500"), Unit.KG);
		ReflectionTestUtils.setField(fruit, "id", id);
		return fruit;
	}

	@Test
	void updateKeepingItsOwnNameIsNotADuplicate() {
		Fruit apple = fruitWithId(1L, "Apple");
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple));
		when(repository.findByName("apple")).thenReturn(Optional.of(apple));

		Fruit updated = service.update(1L, "apple", n("3000"), Unit.KG);

		assertThat(updated.getName()).isEqualTo("apple");
	}

	@Test
	void createsFruitWithNewName() {
		when(repository.findByName("Apple")).thenReturn(Optional.empty());
		when(repository.save(any(Fruit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Fruit created = service.create("  Apple ", n("2500"), Unit.KG);

		assertThat(created.getName()).isEqualTo("Apple");
	}

	@Test
	void doesNotCreateFruitWithDuplicateName() {
		when(repository.findByName("Apple")).thenReturn(Optional.of(apple()));

		assertThatThrownBy(() -> service.create("Apple", n("2500"), Unit.KG))
				.isInstanceOf(DuplicateNameException.class)
				.hasFieldOrPropertyWithValue("existingActive", true);
		verify(repository, never()).save(any());
	}

	@Test
	void duplicateNameOfInactiveFruitSaysItIsInactive() {
		Fruit inactive = apple();
		inactive.deactivate();
		when(repository.findByName("Apple")).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> service.create("Apple", n("2500"), Unit.KG))
				.isInstanceOf(DuplicateNameException.class)
				.hasMessageContaining("inactive")
				.hasFieldOrPropertyWithValue("existingActive", false);
	}

	@Test
	void findByIdFailsWhenMissingOrInactive() {
		when(repository.findActiveById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(1L))
				.isInstanceOf(FruitNotFoundException.class);
	}

	@Test
	void findAllIncludesInactiveOnlyWhenRequested() {
		Fruit inactive = apple();
		inactive.deactivate();
		when(repository.findAllActive()).thenReturn(List.of());
		when(repository.findAll()).thenReturn(List.of(inactive));

		assertThat(service.findAll(false)).isEmpty();
		assertThat(service.findAll(true)).containsExactly(inactive);
	}

	@Test
	void findByIdFindsInactiveFruitOnlyWhenRequested() {
		Fruit inactive = apple();
		inactive.deactivate();
		when(repository.findActiveById(1L)).thenReturn(Optional.empty());
		when(repository.findById(1L)).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> service.findById(1L, false))
				.isInstanceOf(FruitNotFoundException.class);
		assertThat(service.findById(1L, true)).isSameAs(inactive);
	}

	@Test
	void updatesFruitData() {
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple()));
		when(repository.findByName("Red apple")).thenReturn(Optional.empty());

		Fruit updated = service.update(1L, "Red apple", n("3000"), Unit.PIECE);

		assertThat(updated.getName()).isEqualTo("Red apple");
		assertThat(updated.getPrice()).isEqualByComparingTo("3000");
		assertThat(updated.getUnit()).isEqualTo(Unit.PIECE);
	}

	@Test
	void doesNotUpdateToAnotherFruitsName() {
		when(repository.findActiveById(1L)).thenReturn(Optional.of(apple()));
		when(repository.findByName("Pear")).thenReturn(Optional.of(fruitWithId(2L, "Pear")));

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
