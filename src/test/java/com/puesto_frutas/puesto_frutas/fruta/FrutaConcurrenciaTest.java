package com.puesto_frutas.puesto_frutas.fruta;

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

/**
 * Sin la transacción del test: cada llamada al service o al repository confirma
 * por su cuenta, como dos peticiones HTTP distintas. Por eso se limpia a mano.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(FrutaService.class)
class FrutaConcurrenciaTest {

	@Autowired
	private FrutaService service;

	/** Spy: se comporta como el repository real, pero deja meter código en medio. */
	@MockitoSpyBean
	private FrutaRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	void limpiar() {
		repository.deleteAll();
	}

	@Test
	void cadaCambioSubeLaVersion() {
		Long id = repository.save(new Fruta("Manzana", new BigDecimal("1000"), Unidad.KG)).getId();
		assertThat(repository.findById(id).orElseThrow().getVersion()).isZero();

		service.agregarStock(id, new BigDecimal("5"));

		assertThat(repository.findById(id).orElseThrow().getVersion()).isEqualTo(1L);
	}

	@Test
	void unaVentaConDatosViejosNoPisaLaOtra() {
		Long id = repository.save(new Fruta("Manzana", new BigDecimal("1000"), Unidad.KG)).getId();
		service.agregarStock(id, new BigDecimal("10"));

		// Caja 1 lee la fruta (stock 10) y, antes de guardar, la caja 2 vende 4
		Fruta leidaPorCaja1 = repository.findById(id).orElseThrow();
		service.descontarStock(id, new BigDecimal("4"));

		// Caja 1 vende 3 sobre su copia vieja: sin @Version dejaría 7 y se perdería la venta de 4
		leidaPorCaja1.descontarStock(new BigDecimal("3"));
		assertThatThrownBy(() -> repository.save(leidaPorCaja1))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		assertThat(repository.findById(id).orElseThrow().getCantidad()).isEqualByComparingTo("6");
	}

	@Test
	void unaVentaQueChocaSeReintentaYNoSePierdeNinguna() {
		Long id = repository.save(new Fruta("Manzana", new BigDecimal("1000"), Unidad.KG)).getId();
		service.agregarStock(id, new BigDecimal("10"));
		clearInvocations(repository); // contar solo las lecturas de la venta

		// Transacción aparte (REQUIRES_NEW) = otra petición que confirma por su cuenta
		TransactionTemplate otraPeticion = new TransactionTemplate(transactionManager);
		otraPeticion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		AtomicBoolean primeraLectura = new AtomicBoolean(true);
		doAnswer(invocacion -> {
			// Un spy de repository (interfaz) no admite callRealMethod: se lee con findById, que es real
			Optional<Fruta> leida = repository.findById(id).filter(Fruta::isActiva);
			if (primeraLectura.getAndSet(false)) {
				// La caja 1 ya leyó (stock 10); antes de que guarde, la caja 2 vende 4
				otraPeticion.executeWithoutResult(estado -> service.descontarStock(id, new BigDecimal("4")));
			}
			return leida;
		}).when(repository).findByIdAndActivaTrue(any());

		// La caja 1 vende 3: el primer intento choca, el reintento lee stock 6 y vende sobre eso
		Fruta fruta = service.descontarStock(id, new BigDecimal("3"));

		assertThat(fruta.getCantidad()).isEqualByComparingTo("3");
		assertThat(repository.findById(id).orElseThrow().getCantidad()).isEqualByComparingTo("3");
		// 1.ª lectura de la caja 1 + lectura de la caja 2 + reintento de la caja 1
		verify(repository, times(3)).findByIdAndActivaTrue(id);
	}

}
