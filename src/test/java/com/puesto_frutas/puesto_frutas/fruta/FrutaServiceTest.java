package com.puesto_frutas.puesto_frutas.fruta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrutaServiceTest {

	@Mock
	private FrutaRepository repository;

	@InjectMocks
	private FrutaService service;

	private static BigDecimal n(String valor) {
		return new BigDecimal(valor);
	}

	private static Fruta manzana() {
		return new Fruta("Manzana", n("2500"), Unidad.KG);
	}

	@Test
	void creaUnaFrutaConNombreNuevo() {
		when(repository.existsByNombreIgnoreCase("Manzana")).thenReturn(false);
		when(repository.save(any(Fruta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		Fruta creada = service.crear("  Manzana ", n("2500"), Unidad.KG);

		assertThat(creada.getNombre()).isEqualTo("Manzana");
	}

	@Test
	void noCreaUnaFrutaConNombreRepetido() {
		when(repository.existsByNombreIgnoreCase("Manzana")).thenReturn(true);

		assertThatThrownBy(() -> service.crear("Manzana", n("2500"), Unidad.KG))
				.isInstanceOf(NombreDuplicadoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void buscarFallaSiNoExisteOEstaInactiva() {
		when(repository.findByIdAndActivaTrue(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(1L))
				.isInstanceOf(FrutaNoEncontradaException.class);
	}

	@Test
	void actualizaLosDatosDeLaFruta() {
		when(repository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(manzana()));
		when(repository.existsByNombreIgnoreCaseAndIdNot("Manzana roja", 1L)).thenReturn(false);

		Fruta actualizada = service.actualizar(1L, "Manzana roja", n("3000"), Unidad.UNIDAD);

		assertThat(actualizada.getNombre()).isEqualTo("Manzana roja");
		assertThat(actualizada.getPrecio()).isEqualByComparingTo("3000");
		assertThat(actualizada.getUnidad()).isEqualTo(Unidad.UNIDAD);
	}

	@Test
	void noActualizaAUnNombreDeOtraFruta() {
		when(repository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(manzana()));
		when(repository.existsByNombreIgnoreCaseAndIdNot("Pera", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.actualizar(1L, "Pera", n("3000"), Unidad.KG))
				.isInstanceOf(NombreDuplicadoException.class);
	}

	@Test
	void mueveElStockDeLaFruta() {
		when(repository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(manzana()));

		service.agregarStock(1L, n("3"));
		Fruta fruta = service.descontarStock(1L, n("1.5"));

		assertThat(fruta.getCantidad()).isEqualByComparingTo("1.5");
	}

	@Test
	void desactivaYReactivaLaFruta() {
		Fruta fruta = manzana();
		when(repository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(fruta));
		when(repository.findById(1L)).thenReturn(Optional.of(fruta));

		service.desactivar(1L);
		assertThat(fruta.isActiva()).isFalse();

		service.activar(1L);
		assertThat(fruta.isActiva()).isTrue();
	}

}
