package com.puesto_frutas.puesto_frutas.fruta;

public class FrutaNoEncontradaException extends RuntimeException {

	public FrutaNoEncontradaException(Long id) {
		super("No existe una fruta con id " + id);
	}

}
