package com.fruitstand.fruit.domain;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fruitstand.fruit.domain.exception.InsufficientStockException;

/**
 * Sin setters: los datos solo cambian con los métodos de abajo, que validan
 * las reglas. Así el stock nunca puede quedar negativo desde ninguna parte.
 */
@Entity
@Table(name = "fruit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // lo exige JPA
public class Fruit {

	private static final int PRICE_DECIMALS = 2;
	private static final int QUANTITY_DECIMALS = 3;
	private static final int NAME_MAX_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = NAME_MAX_LENGTH, unique = true)
	private String name;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal quantity = BigDecimal.ZERO;

	// En MySQL, Hibernate mapea los enum a una columna ENUM; schema.sql usa VARCHAR
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Unit unit;

	@Column(nullable = false)
	private boolean active = true;

	/**
	 * Bloqueo optimista: Hibernate la sube en cada UPDATE y solo guarda si nadie
	 * la cambió desde que se leyó la fruta. Si no, lanza un error en vez de pisar
	 * el cambio del otro (p. ej. dos ventas a la vez).
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	public Fruit(String name, BigDecimal price, Unit unit) {
		changeName(name);
		changePrice(price);
		changeUnit(unit);
	}

	public void changeName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Name is required");
		}
		String trimmed = name.strip();
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new IllegalArgumentException("Name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		this.name = trimmed;
	}

	public void changePrice(BigDecimal price) {
		if (price == null || price.signum() < 0) {
			throw new IllegalArgumentException("Price must be greater than or equal to 0");
		}
		if (decimals(price) > PRICE_DECIMALS) {
			throw new IllegalArgumentException("Price allows at most " + PRICE_DECIMALS + " decimal places");
		}
		this.price = price;
	}

	/** Se permite con stock, salvo pasar a PIECE con un stock fraccionado (p. ej. 1.5). */
	public void changeUnit(Unit unit) {
		if (unit == null) {
			throw new IllegalArgumentException("Unit is required");
		}
		if (unit == Unit.PIECE && decimals(quantity) > 0) {
			throw new IllegalArgumentException(
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
			throw new IllegalArgumentException("Quantity must be greater than 0");
		}
		if (decimals(quantity) > QUANTITY_DECIMALS) {
			throw new IllegalArgumentException("Quantity allows at most " + QUANTITY_DECIMALS + " decimal places");
		}
		if (unit == Unit.PIECE && decimals(quantity) > 0) {
			throw new IllegalArgumentException("A fruit sold by PIECE only accepts whole quantities");
		}
	}

	private static int decimals(BigDecimal value) {
		return Math.max(0, value.stripTrailingZeros().scale());
	}

}
