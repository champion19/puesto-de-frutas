package com.fruitstand.fruit.web;

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

import com.fruitstand.fruit.application.FruitService;
import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.Unit;
import com.fruitstand.fruit.domain.exception.DuplicateNameException;
import com.fruitstand.fruit.domain.exception.FruitNotFoundException;
import com.fruitstand.fruit.domain.exception.InsufficientStockException;

@WebMvcTest(FruitController.class)
class FruitControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FruitService service;

	private static Fruit apple() {
		return new Fruit("Apple", new BigDecimal("2500"), Unit.KG);
	}

	@Test
	void listsFruits() throws Exception {
		when(service.findAll(false)).thenReturn(List.of(apple()));

		mvc.perform(get("/api/fruits"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Apple"))
				.andExpect(jsonPath("$[0].unit").value("KG"));
	}

	@Test
	void listsInactiveFruitsWhenRequested() throws Exception {
		Fruit inactive = apple();
		inactive.deactivate();
		when(service.findAll(true)).thenReturn(List.of(inactive));

		mvc.perform(get("/api/fruits").param("includeInactive", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].active").value(false));
	}

	@Test
	void getsInactiveFruitWhenRequested() throws Exception {
		Fruit inactive = apple();
		inactive.deactivate();
		when(service.findById(1L, true)).thenReturn(inactive);

		mvc.perform(get("/api/fruits/1").param("includeInactive", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void rejectsNonBooleanIncludeInactive() throws Exception {
		mvc.perform(get("/api/fruits").param("includeInactive", "maybe"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createsFruitAndReturns201() throws Exception {
		when(service.create(eq("Apple"), any(), eq(Unit.KG))).thenReturn(apple());

		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Apple", "price": 2500, "unit": "KG"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.name").value("Apple"));
	}

	@Test
	void rejectsIncompleteDataWith400() throws Exception {
		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "", "unit": "KG"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.price").exists());
	}

	@Test
	void rejectsUnknownUnitNamingTheField() throws Exception {
		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Apple", "price": 2500, "unit": "LB"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Invalid data"))
				.andExpect(jsonPath("$.errors.unit").value("invalid value 'LB', accepted values: PIECE, KG"));
	}

	@Test
	void rejectsNonNumericPriceNamingTheField() throws Exception {
		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Apple", "price": "cheap", "unit": "KG"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.price").value("must be a number"));
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
	}

	@Test
	void returns404WhenFruitDoesNotExist() throws Exception {
		when(service.findById(9L, false)).thenThrow(new FruitNotFoundException(9L));

		mvc.perform(get("/api/fruits/9"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("No fruit found with id 9"));
	}

	@Test
	void returns409WhenNameIsDuplicate() throws Exception {
		Fruit inactive = apple();
		inactive.deactivate();
		when(service.create(any(), any(), any())).thenThrow(new DuplicateNameException(inactive));

		mvc.perform(post("/api/fruits").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Apple", "price": 2500, "unit": "KG"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.existingActive").value(false))
				.andExpect(jsonPath("$.existingId").hasJsonPath());
	}

	@Test
	void returns400WhenStockIsInsufficient() throws Exception {
		when(service.removeStock(eq(1L), any()))
				.thenThrow(new InsufficientStockException("Apple", BigDecimal.ONE, BigDecimal.TEN));

		mvc.perform(post("/api/fruits/1/stock/remove").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity": 10}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns409WhenAnotherRequestModifiedTheFruitConcurrently() throws Exception {
		when(service.removeStock(eq(1L), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Fruit.class, 1L));

		mvc.perform(post("/api/fruits/1/stock/remove").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity": 1}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The fruit was modified by another operation, please try again"));
	}

	@Test
	void deactivatesWithDeleteAndReturns204() throws Exception {
		mvc.perform(delete("/api/fruits/1"))
				.andExpect(status().isNoContent());
	}

}
