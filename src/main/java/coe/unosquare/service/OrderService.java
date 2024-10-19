package coe.unosquare.service;

import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import coe.unosquare.model.ApiResponse;
import coe.unosquare.model.Order;
import coe.unosquare.model.OrderMatcher;
import coe.unosquare.model.OrderStats;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class OrderService {
	@Autowired
	private OrderMatcher orderMatcher;

	public Mono<ApiResponse> viewOrders() {
		// Obteniendo las órdenes en un Mono sin bloquear
		return orderMatcher.getAllOrders();
	}

	public Mono<ResponseEntity<ApiResponse>> processOrder(Order order) {
		// Agregar la orden de manera reactiva y luego intentar emparejarla de forma
		// continua hasta que se procese
		return orderMatcher.addOrder(order).then(orderMatcher.matchOrder(order)) // Procesa la orden de manera reactiva
				.map(processedOrder -> ResponseEntity.ok().body(new ApiResponse(true, "Order processed", order)))
				.onErrorResume(TimeoutException.class,
						ex -> Mono.just(ResponseEntity.status(500).body(
								new ApiResponse(false, "There is no matching order to complete your order", order))))
				.subscribeOn(Schedulers.boundedElastic()); // Utilizando un scheduler para no bloquear el hilo
	}

	public Mono<OrderStats> getOrderStats() {
		// Obteniendo las estadísticas de las órdenes en un Mono sin bloquear
		return orderMatcher.getStatistics();
	}
}
