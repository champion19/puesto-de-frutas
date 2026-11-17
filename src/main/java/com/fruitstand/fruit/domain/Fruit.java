package com.fruitstand.fruit.domain;

import java.math.BigDecimal;

import lombok.Getter;

import com.fruitstand.fruit.domain.exception.InsufficientStockException;
import com.fruitstand.fruit.domain.exception.InvalidFruitException;

/**
 * Una fruta del puesto y sus reglas. Es Java puro: no sabe de base de datos,
 * HTTP ni Spring. Cómo se guarda en MySQL lo decide platform.persistence.FruitEntity.
 *
 * Sin setters: los datos solo cambian con los métodos de abajo, que validan
 * las reglas. Así el stock nunca puede quedar negativo desde ninguna parte.
 */
@Getter
public class Fruit {

	private static final int PRICE_DECIMALS = 2;
	private static final int QUANTITY_DECIMALS = 3;
	private static final int NAME_MAX_LENGTH = 100;

	/** null hasta que la fruta se guarda por primera vez. */
	private Long id;

	private String name;

	private BigDecimal price;

	private BigDecimal quantity = BigDecimal.ZERO;

	private Unit unit;

	private boolean active = true;

	/**
	 * Cuántas veces se ha modificado la fruta guardada (null si es nueva).
	 * Viaja con la fruta para detectar si otra operación la cambió mientras
	 * tanto (p. ej. dos ventas a la vez); el dominio solo la transporta.
	 */
	private Long version;

	/** Fruta nueva: valida todos los datos y empieza activa y sin stock. */
	public Fruit(String name, BigDecimal price, Unit unit) {
		changeName(name);
		changePrice(price);
		changeUnit(unit);
	}

	private Fruit(Long id, String name, BigDecimal price, BigDecimal quantity, Unit unit, boolean active,
			Long version) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.quantity = quantity;
		this.unit = unit;
		this.active = active;
		this.version = version;
	}

	/**
	 * Reconstruye una fruta que ya existía (p. ej. leída de la base). No vuelve a
	 * validar: esos datos ya pasaron las reglas cuando se guardaron.
	 */
	public static Fruit restore(Long id, String name, BigDecimal price, BigDecimal quantity, Unit unit,
			boolean active, Long version) {
		return new Fruit(id, name, price, quantity, unit, active, version);
	}

	public void changeName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidFruitException("Name is required");
		}
		String trimmed = name.strip();
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new InvalidFruitException("Name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		this.name = trimmed;
	}

	public void changePrice(BigDecimal price) {
		if (price == null || price.signum() < 0) {
			throw new InvalidFruitException("Price must be greater than or equal to 0");
		}
		if (decimals(price) > PRICE_DECIMALS) {
			throw new InvalidFruitException("Price allows at most " + PRICE_DECIMALS + " decimal places");
		}
		this.price = price;
	}

	/** Se permite con stock, salvo pasar a PIECE con un stock fraccionado (p. ej. 1.5). */
	public void changeUnit(Unit unit) {
		if (unit == null) {
			throw new InvalidFruitException("Unit is required");
		}
		if (unit == Unit.PIECE && decimals(quantity) > 0) {
			throw new InvalidFruitException(
					"Cannot switch to PIECE with fractional stock: " + quantity.toPlainString());
		}
		this.unit = unit;
	}

	public void addStock(BigDecimal quantity) {
		validateQuantity(quantity);
		this.quantity = this.quantity.add(quantity);
	}

	public void removeStock(BigDecimal quantity) {
		validateQuantity(quantity);
		if (quantity.compareTo(this.quantity) > 0) {
			throw new InsufficientStockException(name, this.quantity, quantity);
		}
		this.quantity = this.quantity.subtract(quantity);
	}

	/** Borrado lógico: la fruta deja de ofrecerse pero conserva su historial. */
	public void deactivate() {
		this.active = false;
	}

	public void activate() {
		this.active = true;
	}

	private void validateQuantity(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new InvalidFruitException("Quantity must be greater than 0");
		}
		if (decimals(quantity) > QUANTITY_DECIMALS) {
			throw new InvalidFruitException("Quantity allows at most " + QUANTITY_DECIMALS + " decimal places");
		}
		if (unit == Unit.PIECE && decimals(quantity) > 0) {
			throw new InvalidFruitException("A fruit sold by PIECE only accepts whole quantities");
		}
	}

	private static int decimals(BigDecimal value) {
		return Math.max(0, value.stripTrailingZeros().scale());
	}

}
