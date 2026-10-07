package com.puesto_frutas.puesto_frutas.fruta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FrutaTest {

	private static BigDecimal n(String valor) {
		return new BigDecimal(valor);
	}

	@Test
	void unaFrutaNuevaEmpiezaSinStockYActiva() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);

		assertThat(fruta.getCantidad()).isEqualByComparingTo("0");
		assertThat(fruta.isActiva()).isTrue();
	}

	@Test
	void quitaLosEspaciosDelNombre() {
		Fruta fruta = new Fruta("  Manzana  ", n("2500"), Unidad.KG);

		assertThat(fruta.getNombre()).isEqualTo("Manzana");
	}

	@Test
	void rechazaNombreVacio() {
		assertThatThrownBy(() -> new Fruta("   ", n("2500"), Unidad.KG))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aceptaPrecioCeroPeroNoNegativo() {
		Fruta fruta = new Fruta("Manzana", n("0"), Unidad.KG);

		assertThat(fruta.getPrecio()).isEqualByComparingTo("0");
		assertThatThrownBy(() -> fruta.cambiarPrecio(n("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rechazaPrecioConMasDeDosDecimales() {
		assertThatThrownBy(() -> new Fruta("Manzana", n("10.555"), Unidad.KG))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void agregaYDescuentaStockConDecimalesEnKg() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);

		fruta.agregarStock(n("3"));
		fruta.descontarStock(n("1.5"));

		assertThat(fruta.getCantidad()).isEqualByComparingTo("1.5");
	}

	@Test
	void noDejaElStockNegativo() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);
		fruta.agregarStock(n("1"));

		assertThatThrownBy(() -> fruta.descontarStock(n("1.5")))
				.isInstanceOf(StockInsuficienteException.class);
		assertThat(fruta.getCantidad()).isEqualByComparingTo("1");
	}

	@Test
	void rechazaCantidadesCeroONegativas() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);

		assertThatThrownBy(() -> fruta.agregarStock(n("0")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> fruta.descontarStock(n("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rechazaCantidadConMasDeTresDecimales() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);

		assertThatThrownBy(() -> fruta.agregarStock(n("1.0005")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void porUnidadSoloAdmiteCantidadesEnteras() {
		Fruta fruta = new Fruta("Piña", n("4000"), Unidad.UNIDAD);

		fruta.agregarStock(n("2.000"));

		assertThat(fruta.getCantidad()).isEqualByComparingTo("2");
		assertThatThrownBy(() -> fruta.agregarStock(n("0.5")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void permiteCambiarLaUnidadConStock() {
		Fruta fruta = new Fruta("Piña", n("4000"), Unidad.UNIDAD);
		fruta.agregarStock(n("3"));

		fruta.cambiarUnidad(Unidad.KG);

		assertThat(fruta.getUnidad()).isEqualTo(Unidad.KG);
		assertThat(fruta.getCantidad()).isEqualByComparingTo("3");
	}

	@Test
	void noPasaAUnidadConStockFraccionado() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);
		fruta.agregarStock(n("1.5"));

		assertThatThrownBy(() -> fruta.cambiarUnidad(Unidad.UNIDAD))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(fruta.getUnidad()).isEqualTo(Unidad.KG);
	}

	@Test
	void seDesactivaYSeReactiva() {
		Fruta fruta = new Fruta("Manzana", n("2500"), Unidad.KG);

		fruta.desactivar();
		assertThat(fruta.isActiva()).isFalse();

		fruta.activar();
		assertThat(fruta.isActiva()).isTrue();
	}

}
