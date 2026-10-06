package com.puesto_frutas.puesto_frutas.fruta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FrutaTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void guardaYLeeUnaFruta() {
		Fruta fruta = new Fruta();
		fruta.setNombre("Manzana");
		fruta.setPrecio(new BigDecimal("2500.50"));
		fruta.setUnidad(Unidad.KG);

		Long id = entityManager.persistAndGetId(fruta, Long.class);
		entityManager.clear();

		Fruta guardada = entityManager.find(Fruta.class, id);
		assertThat(guardada.getNombre()).isEqualTo("Manzana");
		assertThat(guardada.getPrecio()).isEqualByComparingTo("2500.50");
		assertThat(guardada.getCantidad()).isZero();
		assertThat(guardada.getUnidad()).isEqualTo(Unidad.KG);
	}

}
