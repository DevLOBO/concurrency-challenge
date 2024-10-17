package coe.unosquare.model;

public record OrdersProcessed(Order currentOrder, Order buyOrder, Order sellOrder) {
	public OrdersProcessed(Order currentOrder) {
		this(currentOrder, null, null);
	}
}
