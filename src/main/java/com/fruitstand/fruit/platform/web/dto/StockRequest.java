package com.fruitstand.fruit.platform.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record StockRequest(@NotNull BigDecimal quantity) {
}
