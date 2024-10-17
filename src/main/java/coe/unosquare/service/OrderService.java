package coe.unosquare.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import coe.unosquare.model.ApiResponse;
import coe.unosquare.model.Order;
import coe.unosquare.model.OrderMatcher;
import coe.unosquare.model.OrderStats;
import coe.unosquare.model.OrderTimeoutException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class OrderService {
	private final OrderMatcher orderMatcher = new OrderMatcher();

	public Mono<ApiResponse> viewOrders() {
		// Obteniendo las órdenes en un Mono sin bloquear
		return orderMatcher.getAllOrders();
	}

	public Mono<ResponseEntity<ApiResponse>> processOrder(Order order) {
		// Agregar la orden de manera reactiva y luego intentar emparejarla de forma
		// continua hasta que se procese
		return orderMatcher.addOrder(order).then(orderMatcher.matchOrder(order)) // Procesa la orden de manera reactiva
				.map(processedOrder -> ResponseEntity.ok().body(new ApiResponse(true, "Order processed", order)))
				.switchIfEmpty(Mono.error(new OrderTimeoutException()))
				.onErrorResume(OrderTimeoutException.class,
						ex -> Mono
								.just(ResponseEntity.status(500).body(new ApiResponse(false, ex.getMessage(), order))))
				.subscribeOn(Schedulers.boundedElastic()); // Utilizando un scheduler para no bloquear el hilo
	}

	public Mono<OrderStats> getOrderStats() {
		// Obteniendo las estadísticas de las órdenes en un Mono sin bloquear
		return orderMatcher.getStatistics();
	}
}
