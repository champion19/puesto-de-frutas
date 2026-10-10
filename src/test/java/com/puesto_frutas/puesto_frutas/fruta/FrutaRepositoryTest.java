package com.puesto_frutas.puesto_frutas.fruta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FrutaRepositoryTest {

	@Autowired
	private FrutaRepository repository;

	private Fruta guardar(String nombre, boolean activa) {
		Fruta fruta = new Fruta(nombre, new BigDecimal("1000"), Unidad.KG);
		if (!activa) {
			fruta.desactivar();
		}
		return repository.save(fruta);
	}

	@Test
	void listaSoloLasActivasOrdenadasPorNombre() {
		guardar("Pera", true);
		guardar("Banano", false);
		guardar("Manzana", true);

		assertThat(repository.findByActivaTrueOrderByNombreAsc())
				.extracting(Fruta::getNombre)
				.containsExactly("Manzana", "Pera");
	}

	@Test
	void noEncuentraPorIdUnaFrutaDesactivada() {
		Long activa = guardar("Pera", true).getId();
		Long inactiva = guardar("Banano", false).getId();

		assertThat(repository.findByIdAndActivaTrue(activa)).isPresent();
		assertThat(repository.findByIdAndActivaTrue(inactiva)).isEmpty();
	}

	@Test
	void detectaNombreRepetidoSinDistinguirMayusculas() {
		guardar("Manzana", true);
		guardar("Banano", false);

		assertThat(repository.existsByNombreIgnoreCase("MANZANA")).isTrue();
		assertThat(repository.existsByNombreIgnoreCase("banano")).isTrue();
		assertThat(repository.existsByNombreIgnoreCase("Pera")).isFalse();
	}

	@Test
	void alEditarIgnoraElNombreDeLaMismaFruta() {
		Long manzana = guardar("Manzana", true).getId();
		Long pera = guardar("Pera", true).getId();

		assertThat(repository.existsByNombreIgnoreCaseAndIdNot("manzana", manzana)).isFalse();
		assertThat(repository.existsByNombreIgnoreCaseAndIdNot("manzana", pera)).isTrue();
	}

}
