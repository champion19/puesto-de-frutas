package com.fruitstand.fruit.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
 * Casos de uso de la fruta. Las reglas de UNA fruta viven en el dominio (Fruit);
 * aquí se busca, se valida lo que depende de otras frutas (nombre único) y se
 * guarda siempre con repository.save(...): no depende de ningún truco de JPA.
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

	/** Por defecto solo el catálogo; con includeInactive también las desactivadas. */
	@Transactional(readOnly = true)
	public List<Fruit> findAll(boolean includeInactive) {
		return includeInactive ? repository.findAll() : repository.findAllActive();
	}

	/** Solo activas: es la que usan las operaciones que modifican una fruta. */
	@Transactional(readOnly = true)
	public Fruit findById(Long id) {
		return findById(id, false);
	}

	@Transactional(readOnly = true)
	public Fruit findById(Long id, boolean includeInactive) {
		Optional<Fruit> fruit = includeInactive ? repository.findById(id) : repository.findActiveById(id);
		return fruit.orElseThrow(() -> new FruitNotFoundException(id));
	}

	public Fruit create(String name, BigDecimal price, Unit unit) {
		Fruit fruit = new Fruit(name, price, unit);
		repository.findByName(fruit.getName()).ifPresent(existing -> {
			throw new DuplicateNameException(existing);
		});
		return repository.save(fruit);
	}

	public Fruit update(Long id, String name, BigDecimal price, Unit unit) {
		Fruit fruit = findById(id);
		// El nombre puede repetirse solo si es el de la misma fruta.
		if (name != null) {
			repository.findByName(name.strip())
					.filter(existing -> !existing.getId().equals(id))
					.ifPresent(existing -> {
						throw new DuplicateNameException(existing);
					});
		}
		fruit.changeName(name);
		fruit.changePrice(price);
		fruit.changeUnit(unit);
		return repository.save(fruit);
	}

	public Fruit addStock(Long id, BigDecimal quantity) {
		Fruit fruit = findById(id);
		fruit.addStock(quantity);
		return repository.save(fruit);
	}

	public Fruit removeStock(Long id, BigDecimal quantity) {
		Fruit fruit = findById(id);
		fruit.removeStock(quantity);
		return repository.save(fruit);
	}

	public void deactivate(Long id) {
		Fruit fruit = findById(id);
		fruit.deactivate();
		repository.save(fruit);
	}

	/** Incluye las inactivas porque la fruta a reactivar lo está. */
	public Fruit activate(Long id) {
		Fruit fruit = findById(id, true);
		fruit.activate();
		return repository.save(fruit);
	}

}
