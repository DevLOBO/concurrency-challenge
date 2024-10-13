package coe.unosquare.model;

import java.util.List;

public record AvailableOrdersOrdersInProcess(List<Order> buyOrders, List<Order> sellOrders) {

}
