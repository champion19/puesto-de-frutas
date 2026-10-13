package com.fruitstand.fruit.domain.exception;

public class DuplicateNameException extends RuntimeException {

	public DuplicateNameException(String name) {
		super("A fruit named " + name + " already exists");
	}

}
