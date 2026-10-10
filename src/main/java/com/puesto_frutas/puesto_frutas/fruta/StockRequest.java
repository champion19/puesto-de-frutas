package com.puesto_frutas.puesto_frutas.fruta;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record StockRequest(@NotNull BigDecimal cantidad) {
}
