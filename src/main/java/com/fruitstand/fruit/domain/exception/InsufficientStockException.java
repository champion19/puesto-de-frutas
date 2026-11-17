package com.fruitstand.fruit.domain.exception;

import java.math.BigDecimal;

public class InsufficientStockException extends RuntimeException {

	public InsufficientStockException(String fruit, BigDecimal available, BigDecimal requested) {
		super("Insufficient stock of " + fruit + ": " + available.toPlainString()
				+ " available, " + requested.toPlainString() + " requested");
	}

}
