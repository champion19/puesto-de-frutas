package com.puesto_frutas.puesto_frutas.fruta;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sin setters: los datos solo cambian con los métodos de abajo, que validan
 * las reglas. Así el stock nunca puede quedar negativo desde ninguna parte.
 */
@Entity
@Table(name = "fruta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // lo exige JPA
public class Fruta {

	private static final int DECIMALES_PRECIO = 2;
	private static final int DECIMALES_CANTIDAD = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100, unique = true)
	private String nombre;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal cantidad = BigDecimal.ZERO;

	// En MySQL, Hibernate mapea los enum a una columna ENUM; schema.sql usa VARCHAR
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Unidad unidad;

	@Column(nullable = false)
	private boolean activa = true;

	public Fruta(String nombre, BigDecimal precio, Unidad unidad) {
		cambiarNombre(nombre);
		cambiarPrecio(precio);
		cambiarUnidad(unidad);
	}

	public void cambiarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre es obligatorio");
		}
		this.nombre = nombre.strip();
	}

	public void cambiarPrecio(BigDecimal precio) {
		if (precio == null || precio.signum() < 0) {
			throw new IllegalArgumentException("El precio debe ser mayor o igual que 0");
		}
		if (decimales(precio) > DECIMALES_PRECIO) {
			throw new IllegalArgumentException("El precio admite máximo " + DECIMALES_PRECIO + " decimales");
		}
		this.precio = precio;
	}

	/** Se permite con stock, salvo pasar a UNIDAD con un stock fraccionado (p. ej. 1.5). */
	public void cambiarUnidad(Unidad unidad) {
		if (unidad == null) {
			throw new IllegalArgumentException("La unidad es obligatoria");
		}
		if (unidad == Unidad.UNIDAD && decimales(cantidad) > 0) {
			throw new IllegalArgumentException(
					"No se puede pasar a UNIDAD con un stock fraccionado: " + cantidad.toPlainString());
		}
		this.unidad = unidad;
	}

	public void agregarStock(BigDecimal cantidad) {
		validarCantidad(cantidad);
		this.cantidad = this.cantidad.add(cantidad);
	}

	public void descontarStock(BigDecimal cantidad) {
		validarCantidad(cantidad);
		if (cantidad.compareTo(this.cantidad) > 0) {
			throw new StockInsuficienteException(nombre, this.cantidad, cantidad);
		}
		this.cantidad = this.cantidad.subtract(cantidad);
	}

	/** Borrado lógico: la fruta deja de ofrecerse pero conserva su historial. */
	public void desactivar() {
		this.activa = false;
	}

	public void activar() {
		this.activa = true;
	}

	private void validarCantidad(BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
		}
		if (decimales(cantidad) > DECIMALES_CANTIDAD) {
			throw new IllegalArgumentException("La cantidad admite máximo " + DECIMALES_CANTIDAD + " decimales");
		}
		if (unidad == Unidad.UNIDAD && decimales(cantidad) > 0) {
			throw new IllegalArgumentException("Una fruta por UNIDAD solo admite cantidades enteras");
		}
	}

	private static int decimales(BigDecimal valor) {
		return Math.max(0, valor.stripTrailingZeros().scale());
	}

}
