package com.puesto_frutas.puesto_frutas.fruta;

import java.math.BigDecimal;

/** Lo que ve el cliente: la entidad nunca sale del service hacia la API. */
public record FrutaResponse(Long id, String nombre, BigDecimal precio, BigDecimal cantidad, Unidad unidad,
		boolean activa) {

	public static FrutaResponse de(Fruta fruta) {
		return new FrutaResponse(fruta.getId(), fruta.getNombre(), fruta.getPrecio(), fruta.getCantidad(),
				fruta.getUnidad(), fruta.isActiva());
	}

}
