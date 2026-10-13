package com.fruitstand.fruit.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;
import com.fruitstand.fruit.domain.Unit;
import com.fruitstand.fruit.domain.exception.DuplicateNameException;
import com.fruitstand.fruit.domain.exception.FruitNotFoundException;

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
public class FruitService {

	private final FruitRepository repository;

	@Transactional(readOnly = true)
	public List<Fruit> findAll() {
		return repository.findAllActive();
	}

	@Transactional(readOnly = true)
	public Fruit findById(Long id) {
		return repository.findActiveById(id)
				.orElseThrow(() -> new FruitNotFoundException(id));
	}

	public Fruit create(String name, BigDecimal price, Unit unit) {
		Fruit fruit = new Fruit(name, price, unit);
		if (repository.existsByName(fruit.getName())) {
			throw new DuplicateNameException(fruit.getName());
		}
		return repository.save(fruit);
	}

	public Fruit update(Long id, String name, BigDecimal price, Unit unit) {
		Fruit fruit = findById(id);
		// Se revisa antes de cambiar la entidad: la consulta haría flush del nombre
		// nuevo y la base respondería con uk_fruit_name en vez de este error.
		if (name != null && repository.existsByNameExcludingId(name.strip(), id)) {
			throw new DuplicateNameException(name.strip());
		}
		fruit.changeName(name);
		fruit.changePrice(price);
		fruit.changeUnit(unit);
		return fruit;
	}

	public Fruit addStock(Long id, BigDecimal quantity) {
		Fruit fruit = findById(id);
		fruit.addStock(quantity);
		return fruit;
	}

	public Fruit removeStock(Long id, BigDecimal quantity) {
		Fruit fruit = findById(id);
		fruit.removeStock(quantity);
		return fruit;
	}

	public void deactivate(Long id) {
		findById(id).deactivate();
	}

	/** Usa repository.findById porque la fruta a reactivar está inactiva. */
	public Fruit activate(Long id) {
		Fruit fruit = repository.findById(id)
				.orElseThrow(() -> new FruitNotFoundException(id));
		fruit.activate();
		return fruit;
	}

}
