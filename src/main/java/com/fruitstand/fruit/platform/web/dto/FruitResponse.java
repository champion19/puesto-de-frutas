package com.fruitstand.fruit.web.dto;

import java.math.BigDecimal;

import com.fruitstand.fruit.domain.Fruit;
import com.fruitstand.fruit.domain.Unit;

/** Lo que ve el cliente: la entidad nunca sale del service hacia la API. */
public record FruitResponse(Long id, String name, BigDecimal price, BigDecimal quantity, Unit unit,
		boolean active) {

	public static FruitResponse from(Fruit fruit) {
		return new FruitResponse(fruit.getId(), fruit.getName(), fruit.getPrice(), fruit.getQuantity(),
				fruit.getUnit(), fruit.isActive());
	}

}
