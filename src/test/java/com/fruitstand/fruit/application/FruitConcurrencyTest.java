package com.fruitstand.fruit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.FruitRepository;
import com.fruitstand.fruit.domain.Unit;
import com.fruitstand.fruit.infrastructure.persistence.JpaFruitRepository;
import com.fruitstand.fruit.infrastructure.persistence.SpringDataFruitRepository;

/**
 * Sin la transacción del test: cada llamada al service o al repository confirma
 * por su cuenta, como dos peticiones HTTP distintas. Por eso se limpia a mano.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ FruitService.class, JpaFruitRepository.class })
class FruitConcurrencyTest {

	@Autowired
	private FruitService service;

	/** Spy del puerto: se comporta como el real, pero deja meter código en medio. */
	@MockitoSpyBean
	private FruitRepository repository;

	/** Para preparar y revisar datos sin pasar por el spy (y para deleteAll). */
	@Autowired
	private SpringDataFruitRepository jpa;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	void cleanUp() {
		jpa.deleteAll();
	}

	@Test
	void everyChangeIncrementsTheVersion() {
		Long id = jpa.save(new Fruit("Apple", new BigDecimal("1000"), Unit.KG)).getId();
		assertThat(jpa.findById(id).orElseThrow().getVersion()).isZero();

		service.addStock(id, new BigDecimal("5"));

		assertThat(jpa.findById(id).orElseThrow().getVersion()).isEqualTo(1L);
	}

	@Test
	void saleWithStaleDataDoesNotOverwriteTheOther() {
		Long id = jpa.save(new Fruit("Apple", new BigDecimal("1000"), Unit.KG)).getId();
		service.addStock(id, new BigDecimal("10"));

		// Caja 1 lee la fruta (stock 10) y, antes de guardar, la caja 2 vende 4
		Fruit readByRegister1 = jpa.findById(id).orElseThrow();
		service.removeStock(id, new BigDecimal("4"));

		// Caja 1 vende 3 sobre su copia vieja: sin @Version dejaría 7 y se perdería la venta de 4
		readByRegister1.removeStock(new BigDecimal("3"));
		assertThatThrownBy(() -> jpa.save(readByRegister1))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		assertThat(jpa.findById(id).orElseThrow().getQuantity()).isEqualByComparingTo("6");
	}

	@Test
	void conflictingSaleIsRetriedAndNoSaleIsLost() {
		Long id = jpa.save(new Fruit("Apple", new BigDecimal("1000"), Unit.KG)).getId();
		service.addStock(id, new BigDecimal("10"));
		clearInvocations(repository); // contar solo las lecturas de la venta

		// Transacción aparte (REQUIRES_NEW) = otra petición que confirma por su cuenta
		TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
		otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		AtomicBoolean firstRead = new AtomicBoolean(true);
		doAnswer(invocation -> {
			// Se lee con Spring Data directo para no volver a pasar por el spy
			Optional<Fruit> read = jpa.findById(id).filter(Fruit::isActive);
			if (firstRead.getAndSet(false)) {
				// La caja 1 ya leyó (stock 10); antes de que guarde, la caja 2 vende 4
				otherRequest.executeWithoutResult(status -> service.removeStock(id, new BigDecimal("4")));
			}
			return read;
		}).when(repository).findActiveById(any());

		// La caja 1 vende 3: el primer intento choca, el reintento lee stock 6 y vende sobre eso
		Fruit fruit = service.removeStock(id, new BigDecimal("3"));

		assertThat(fruit.getQuantity()).isEqualByComparingTo("3");
		assertThat(jpa.findById(id).orElseThrow().getQuantity()).isEqualByComparingTo("3");
		// 1.ª lectura de la caja 1 + lectura de la caja 2 + reintento de la caja 1
		verify(repository, times(3)).findActiveById(id);
	}

}
