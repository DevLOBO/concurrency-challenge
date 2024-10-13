package coe.unosquare.model;

import java.util.Objects;
import java.util.Optional;

public class Order {
    public enum OrderType { BUY, SELL }

    private final long id;
    private final OrderType orderType;
    private final double price;
    private final int quantity;
    private final long timestamp;

    public Order(OrderType orderType, double price, int quantity) {
        this.id = System.nanoTime(); // Unique ID based on timestamp
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public Optional<Long> getId() {
        return Optional.of(id);
    }

    public Optional<OrderType> getType() {
        return Optional.ofNullable(orderType);
    }

    public Optional<Double> getPrice() {
        return Optional.of(price);
    }

    public Optional<Integer> getQuantity() {
        return Optional.of(quantity);
    }

    public Optional<Long> getTimestamp() {
        return Optional.of(timestamp);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Order order = (Order) o;
        return id == order.id && Double.compare(price, order.price) == 0 && quantity == order.quantity && timestamp == order.timestamp && orderType == order.orderType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderType, price, quantity, timestamp);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", type=" + orderType +
                ", price=" + price +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                '}';
    }
}