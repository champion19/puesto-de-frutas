package com.fruitstand.fruit.domain.exception;

/**
 * Se rompió una regla de la fruta (precio negativo, demasiados decimales...).
 * Es propia del dominio para no confundirla con un IllegalArgumentException
 * cualquiera, que sería un error del programa y no del dato enviado.
 */
public class InvalidFruitException extends RuntimeException {

	public InvalidFruitException(String message) {
		super(message);
	}

}
