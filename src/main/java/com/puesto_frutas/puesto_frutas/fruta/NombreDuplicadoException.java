package com.puesto_frutas.puesto_frutas.fruta;

public class NombreDuplicadoException extends RuntimeException {

	public NombreDuplicadoException(String nombre) {
		super("Ya existe una fruta llamada " + nombre);
	}

}
