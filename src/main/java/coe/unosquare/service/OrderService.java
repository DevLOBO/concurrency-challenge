package coe.unosquare.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import coe.unosquare.model.ApiResponse;
import coe.unosquare.model.Order;
import coe.unosquare.model.OrderMatcher;
import coe.unosquare.model.OrderTimeoutException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class OrderService {
	private final OrderMatcher orderMatcher = new OrderMatcher();

	public Mono<ApiResponse> viewOrders() {
		return Mono.just(orderMatcher.getAllOrders());
	}

	public Mono<ResponseEntity<ApiResponse>> processOrder(Order order) {
		return Mono.fromCallable(() -> {
			orderMatcher.addOrder(order);
			orderMatcher.matchOrders(order);
			return ResponseEntity.ok().body(new ApiResponse(true, "Order processed", order));
		}).subscribeOn(Schedulers.boundedElastic()).onErrorReturn(OrderTimeoutException.class, ResponseEntity
				.status(400).body(new ApiResponse(false, "There is no order matching to complete your order.", order)));
	}
}