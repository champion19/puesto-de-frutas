package com.puesto_frutas.puesto_frutas.fruta;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/frutas")
@RequiredArgsConstructor
public class FrutaController {

	private final FrutaService service;

	@GetMapping
	public List<FrutaResponse> listar() {
		return service.listar().stream().map(FrutaResponse::de).toList();
	}

	@GetMapping("/{id}")
	public FrutaResponse buscar(@PathVariable Long id) {
		return FrutaResponse.de(service.buscar(id));
	}

	@PostMapping
	public ResponseEntity<FrutaResponse> crear(@Valid @RequestBody FrutaRequest request) {
		Fruta fruta = service.crear(request.nombre(), request.precio(), request.unidad());
		return ResponseEntity.created(URI.create("/api/frutas/" + fruta.getId())).body(FrutaResponse.de(fruta));
	}

	@PutMapping("/{id}")
	public FrutaResponse actualizar(@PathVariable Long id, @Valid @RequestBody FrutaRequest request) {
		return FrutaResponse.de(service.actualizar(id, request.nombre(), request.precio(), request.unidad()));
	}

	/** Borrado lógico: la fruta queda inactiva, no se elimina de la tabla. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		service.desactivar(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/activar")
	public FrutaResponse activar(@PathVariable Long id) {
		return FrutaResponse.de(service.activar(id));
	}

	@PostMapping("/{id}/stock/agregar")
	public FrutaResponse agregarStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
		return FrutaResponse.de(service.agregarStock(id, request.cantidad()));
	}

	@PostMapping("/{id}/stock/descontar")
	public FrutaResponse descontarStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
		return FrutaResponse.de(service.descontarStock(id, request.cantidad()));
	}

}
