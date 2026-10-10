package com.puesto_frutas.puesto_frutas.fruta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FrutaController.class)
class FrutaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FrutaService service;

	private static Fruta manzana() {
		return new Fruta("Manzana", new BigDecimal("2500"), Unidad.KG);
	}

	@Test
	void listaLasFrutas() throws Exception {
		when(service.listar()).thenReturn(List.of(manzana()));

		mvc.perform(get("/api/frutas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Manzana"))
				.andExpect(jsonPath("$[0].unidad").value("KG"));
	}

	@Test
	void creaUnaFrutaYDevuelve201() throws Exception {
		when(service.crear(eq("Manzana"), any(), eq(Unidad.KG))).thenReturn(manzana());

		mvc.perform(post("/api/frutas").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "Manzana", "precio": 2500, "unidad": "KG"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.nombre").value("Manzana"));
	}

	@Test
	void rechazaDatosIncompletosCon400() throws Exception {
		mvc.perform(post("/api/frutas").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "", "unidad": "KG"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.nombre").exists())
				.andExpect(jsonPath("$.errores.precio").exists());
	}

	@Test
	void devuelve404SiLaFrutaNoExiste() throws Exception {
		when(service.buscar(9L)).thenThrow(new FrutaNoEncontradaException(9L));

		mvc.perform(get("/api/frutas/9"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("No existe una fruta con id 9"));
	}

	@Test
	void devuelve409SiElNombreEstaRepetido() throws Exception {
		when(service.crear(any(), any(), any())).thenThrow(new NombreDuplicadoException("Manzana"));

		mvc.perform(post("/api/frutas").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "Manzana", "precio": 2500, "unidad": "KG"}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void devuelve400SiNoHayStockSuficiente() throws Exception {
		when(service.descontarStock(eq(1L), any()))
				.thenThrow(new StockInsuficienteException("Manzana", BigDecimal.ONE, BigDecimal.TEN));

		mvc.perform(post("/api/frutas/1/stock/descontar").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cantidad": 10}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void devuelve409SiOtraPeticionModificoLaFrutaALaVez() throws Exception {
		when(service.descontarStock(eq(1L), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Fruta.class, 1L));

		mvc.perform(post("/api/frutas/1/stock/descontar").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cantidad": 1}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("La fruta fue modificada por otra operación, intenta de nuevo"));
	}

	@Test
	void desactivaConDeleteYDevuelve204() throws Exception {
		mvc.perform(delete("/api/frutas/1"))
				.andExpect(status().isNoContent());
	}

}
