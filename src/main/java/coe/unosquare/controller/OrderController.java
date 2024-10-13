package coe.unosquare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import coe.unosquare.model.ApiResponse;
import coe.unosquare.model.Order;
import coe.unosquare.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
public class OrderController {
	@Autowired
	private OrderService orderService;

	@GetMapping("/view")
	public Mono<ApiResponse> viewOrders() {
		return orderService.viewOrders();
	}

	@PostMapping("/submit")
	public Mono<ResponseEntity<ApiResponse>> submitOrder(@RequestParam("ordertype") Order.OrderType orderType,
			@RequestParam("price") double price, @RequestParam("quantity") int quantity) {
		Order orderRequest = new Order(orderType, price, quantity);
		return orderService.processOrder(orderRequest);
	}
}