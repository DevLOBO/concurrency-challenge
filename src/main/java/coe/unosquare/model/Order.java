package coe.unosquare.model;

import java.util.UUID;

public record Order(UUID id, OrderType orderType, double price, int quantity, long timestamp) {

	public Order(OrderType orderType, double price, int quantity) {
		this(UUID.randomUUID(), orderType, price, quantity, System.currentTimeMillis());
	}

}