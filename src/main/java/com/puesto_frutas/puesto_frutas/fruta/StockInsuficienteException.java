package com.puesto_frutas.puesto_frutas.fruta;

import java.math.BigDecimal;

public class StockInsuficienteException extends RuntimeException {

	public StockInsuficienteException(String fruta, BigDecimal disponible, BigDecimal solicitado) {
		super("Stock insuficiente de " + fruta + ": hay " + disponible.toPlainString()
				+ " y se pidieron " + solicitado.toPlainString());
	}

}
