package com.puesto_frutas.puesto_frutas.fruta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/** Service con la base real (H2): cubre lo que el mock no ve, como el flush de Hibernate. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FrutaService.class)
class FrutaServiceIntegracionTest {

	@Autowired
	private FrutaService service;

	@Autowired
	private FrutaRepository repository;

	@Test
	void renombrarAUnNombreDeOtraFrutaDaNombreDuplicado() {
		repository.saveAndFlush(new Fruta("Manzana", new BigDecimal("1000"), Unidad.KG));
		Long pera = repository.saveAndFlush(new Fruta("Pera", new BigDecimal("1000"), Unidad.KG)).getId();

		assertThatThrownBy(() -> service.actualizar(pera, " Manzana ", new BigDecimal("1000"), Unidad.KG))
				.isInstanceOf(NombreDuplicadoException.class);
	}

}
