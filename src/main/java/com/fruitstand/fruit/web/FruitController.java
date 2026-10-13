package com.fruitstand.fruit.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.fruitstand.fruit.application.FruitService;
import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.web.dto.FruitRequest;
import com.fruitstand.fruit.web.dto.FruitResponse;
import com.fruitstand.fruit.web.dto.StockRequest;

@RestController
@RequestMapping("/api/fruits")
@RequiredArgsConstructor
public class FruitController {

	private final FruitService service;

	/** Catálogo (solo activas). Con ?includeInactive=true también las desactivadas. */
	@GetMapping
	public List<FruitResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
		return service.findAll(includeInactive).stream().map(FruitResponse::from).toList();
	}

	/** Una fruta desactivada responde 404, salvo con ?includeInactive=true (p. ej. para reactivarla). */
	@GetMapping("/{id}")
	public FruitResponse get(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeInactive) {
		return FruitResponse.from(service.findById(id, includeInactive));
	}

	@PostMapping
	public ResponseEntity<FruitResponse> create(@Valid @RequestBody FruitRequest request) {
		Fruit fruit = service.create(request.name(), request.price(), request.unit());
		return ResponseEntity.created(URI.create("/api/fruits/" + fruit.getId())).body(FruitResponse.from(fruit));
	}

	@PutMapping("/{id}")
	public FruitResponse update(@PathVariable Long id, @Valid @RequestBody FruitRequest request) {
		return FruitResponse.from(service.update(id, request.name(), request.price(), request.unit()));
	}

	/** Borrado lógico: la fruta queda inactiva, no se elimina de la tabla. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		service.deactivate(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/activate")
	public FruitResponse activate(@PathVariable Long id) {
		return FruitResponse.from(service.activate(id));
	}

	@PostMapping("/{id}/stock/add")
	public FruitResponse addStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
		return FruitResponse.from(service.addStock(id, request.quantity()));
	}

	@PostMapping("/{id}/stock/remove")
	public FruitResponse removeStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
		return FruitResponse.from(service.removeStock(id, request.quantity()));
	}

}
