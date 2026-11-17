package com.fruitstand.fruit.platform.persistence;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.Unit;

/**
 * La fila de la tabla fruit tal como la ve JPA. No tiene reglas: solo traduce
 * entre la base y el dominio con from(...) y toDomain().
 */
@Entity
@Table(name = "fruit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // lo exige JPA
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FruitEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100, unique = true)
	private String name;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal quantity;

	// En MySQL, Hibernate mapea los enum a una columna ENUM; schema.sql usa VARCHAR
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Unit unit;

	@Column(nullable = false)
	private boolean active;

	/**
	 * Bloqueo optimista: Hibernate la sube en cada UPDATE y solo guarda si nadie
	 * la cambió desde que se leyó la fruta. Si no, lanza un error en vez de pisar
	 * el cambio del otro (p. ej. dos ventas a la vez).
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	/** Dominio → tabla. Con id y versión null, JPA la inserta como nueva. */
	public static FruitEntity from(Fruit fruit) {
		return new FruitEntity(fruit.getId(), fruit.getName(), fruit.getPrice(), fruit.getQuantity(),
				fruit.getUnit(), fruit.isActive(), fruit.getVersion());
	}

	/** Tabla → dominio. */
	public Fruit toDomain() {
		return Fruit.restore(id, name, price, quantity, unit, active, version);
	}

}
