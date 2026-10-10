package com.puesto_frutas.puesto_frutas.fruta;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Las reglas de cada fruta viven en la entidad; aquí solo se busca, se valida
 * lo que depende de otras frutas (nombre único) y se guarda.
 * Los cambios sobre una fruta ya cargada se guardan solos al cerrar la transacción.
 *
 * Si otra petición cambió la misma fruta a la vez (@Version), se reintenta el
 * método completo: transacción nueva, lectura fresca y la operación otra vez.
 * Hasta 3 reintentos con espera de 50, 100 y 200 ms (± 25 ms al azar para que dos
 * reintentos no vuelvan a chocar). Si sigue fallando, sale como 409.
 */
@Service
@Transactional
@Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 50, jitter = 25,
		multiplier = 2)
@RequiredArgsConstructor
public class FrutaService {

	private final FrutaRepository repository;

	@Transactional(readOnly = true)
	public List<Fruta> listar() {
		return repository.findByActivaTrueOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	public Fruta buscar(Long id) {
		return repository.findByIdAndActivaTrue(id)
				.orElseThrow(() -> new FrutaNoEncontradaException(id));
	}

	public Fruta crear(String nombre, BigDecimal precio, Unidad unidad) {
		Fruta fruta = new Fruta(nombre, precio, unidad);
		if (repository.existsByNombreIgnoreCase(fruta.getNombre())) {
			throw new NombreDuplicadoException(fruta.getNombre());
		}
		return repository.save(fruta);
	}

	public Fruta actualizar(Long id, String nombre, BigDecimal precio, Unidad unidad) {
		Fruta fruta = buscar(id);
		// Se revisa antes de cambiar la entidad: la consulta haría flush del nombre
		// nuevo y la base respondería con uk_fruta_nombre en vez de este error.
		if (nombre != null && repository.existsByNombreIgnoreCaseAndIdNot(nombre.strip(), id)) {
			throw new NombreDuplicadoException(nombre.strip());
		}
		fruta.cambiarNombre(nombre);
		fruta.cambiarPrecio(precio);
		fruta.cambiarUnidad(unidad);
		return fruta;
	}

	public Fruta agregarStock(Long id, BigDecimal cantidad) {
		Fruta fruta = buscar(id);
		fruta.agregarStock(cantidad);
		return fruta;
	}

	public Fruta descontarStock(Long id, BigDecimal cantidad) {
		Fruta fruta = buscar(id);
		fruta.descontarStock(cantidad);
		return fruta;
	}

	public void desactivar(Long id) {
		buscar(id).desactivar();
	}

	/** Usa findById porque la fruta a reactivar está inactiva. */
	public Fruta activar(Long id) {
		Fruta fruta = repository.findById(id)
				.orElseThrow(() -> new FrutaNoEncontradaException(id));
		fruta.activar();
		return fruta;
	}

}
