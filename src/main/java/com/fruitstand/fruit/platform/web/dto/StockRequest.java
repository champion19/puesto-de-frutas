package com.fruitstand.fruit.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record StockRequest(@NotNull BigDecimal quantity) {
}
