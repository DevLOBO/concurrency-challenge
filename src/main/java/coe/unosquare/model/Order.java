package coe.unosquare.model;

public record Order(long id, OrderType orderType, double price, int quantity, long timestamp) {

	public Order(OrderType orderType, double price, int quantity) {
		this(System.nanoTime(), orderType, price, quantity, System.currentTimeMillis());
	}

}