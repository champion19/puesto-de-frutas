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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fruta")
@Getter
@Setter
@NoArgsConstructor
public class Fruta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100, unique = true)
	private String nombre;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(nullable = false)
	private Integer cantidad = 0;

	// En MySQL, Hibernate mapea los enum a una columna ENUM; schema.sql usa VARCHAR
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Unidad unidad;

}
