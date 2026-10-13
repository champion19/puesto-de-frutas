package com.fruitstand.fruit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fruitstand.fruit.domain.exception.InsufficientStockException;

class FruitTest {

	private static BigDecimal n(String value) {
		return new BigDecimal(value);
	}

	@Test
	void newFruitStartsWithNoStockAndActive() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);

		assertThat(fruit.getQuantity()).isEqualByComparingTo("0");
		assertThat(fruit.isActive()).isTrue();
	}

	@Test
	void trimsWhitespaceFromName() {
		Fruit fruit = new Fruit("  Apple  ", n("2500"), Unit.KG);

		assertThat(fruit.getName()).isEqualTo("Apple");
	}

	@Test
	void rejectsBlankName() {
		assertThatThrownBy(() -> new Fruit("   ", n("2500"), Unit.KG))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNameLongerThanOneHundredCharacters() {
		Fruit fruit = new Fruit("a".repeat(100), n("2500"), Unit.KG);

		assertThat(fruit.getName()).hasSize(100);
		assertThatThrownBy(() -> fruit.changeName("a".repeat(101)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void acceptsZeroPriceButNotNegative() {
		Fruit fruit = new Fruit("Apple", n("0"), Unit.KG);

		assertThat(fruit.getPrice()).isEqualByComparingTo("0");
		assertThatThrownBy(() -> fruit.changePrice(n("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsPriceWithMoreThanTwoDecimals() {
		assertThatThrownBy(() -> new Fruit("Apple", n("10.555"), Unit.KG))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void addsAndRemovesStockWithDecimalsInKg() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);

		fruit.addStock(n("3"));
		fruit.removeStock(n("1.5"));

		assertThat(fruit.getQuantity()).isEqualByComparingTo("1.5");
	}

	@Test
	void doesNotAllowNegativeStock() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);
		fruit.addStock(n("1"));

		assertThatThrownBy(() -> fruit.removeStock(n("1.5")))
				.isInstanceOf(InsufficientStockException.class);
		assertThat(fruit.getQuantity()).isEqualByComparingTo("1");
	}

	@Test
	void rejectsZeroOrNegativeQuantities() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);

		assertThatThrownBy(() -> fruit.addStock(n("0")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> fruit.removeStock(n("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsQuantityWithMoreThanThreeDecimals() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);

		assertThatThrownBy(() -> fruit.addStock(n("1.0005")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void byPieceOnlyAcceptsWholeQuantities() {
		Fruit fruit = new Fruit("Pineapple", n("4000"), Unit.PIECE);

		fruit.addStock(n("2.000"));

		assertThat(fruit.getQuantity()).isEqualByComparingTo("2");
		assertThatThrownBy(() -> fruit.addStock(n("0.5")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void allowsChangingUnitWithStock() {
		Fruit fruit = new Fruit("Pineapple", n("4000"), Unit.PIECE);
		fruit.addStock(n("3"));

		fruit.changeUnit(Unit.KG);

		assertThat(fruit.getUnit()).isEqualTo(Unit.KG);
		assertThat(fruit.getQuantity()).isEqualByComparingTo("3");
	}

	@Test
	void cannotSwitchToPieceWithFractionalStock() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);
		fruit.addStock(n("1.5"));

		assertThatThrownBy(() -> fruit.changeUnit(Unit.PIECE))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(fruit.getUnit()).isEqualTo(Unit.KG);
	}

	@Test
	void deactivatesAndReactivates() {
		Fruit fruit = new Fruit("Apple", n("2500"), Unit.KG);

		fruit.deactivate();
		assertThat(fruit.isActive()).isFalse();

		fruit.activate();
		assertThat(fruit.isActive()).isTrue();
	}

}
