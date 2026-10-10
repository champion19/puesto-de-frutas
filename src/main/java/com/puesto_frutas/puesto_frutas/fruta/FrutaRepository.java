package com.puesto_frutas.puesto_frutas.fruta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FrutaRepository extends JpaRepository<Fruta, Long> {

	/** Catálogo: solo las frutas que se siguen ofreciendo. */
	List<Fruta> findByActivaTrueOrderByNombreAsc();

	Optional<Fruta> findByIdAndActivaTrue(Long id);

	/** Incluye las desactivadas: el nombre sigue ocupado en la tabla (uk_fruta_nombre). */
	boolean existsByNombreIgnoreCase(String nombre);

	/** Para editar: el nombre puede repetirse solo si es el de la misma fruta. */
	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

}
