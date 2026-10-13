package com.fruitstand.fruit.domain.exception;

public class FruitNotFoundException extends RuntimeException {

	public FruitNotFoundException(Long id) {
		super("No fruit found with id " + id);
	}

}
