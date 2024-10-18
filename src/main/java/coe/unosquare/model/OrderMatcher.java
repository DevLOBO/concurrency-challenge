package coe.unosquare.model;

import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

public class OrderMatcher {
	private static final int SLEEPING_TIME = 100;
	private static final long TIMEOUT = 10;

	private static final Comparator<Order> buyComparator = Comparator.comparing((Order order) -> -1 * order.price())
			.thenComparing(Order::timestamp);
	private static final Comparator<Order> sellComparator = Comparator.comparing(Order::price)
			.thenComparing(Order::timestamp);

	private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(100, buyComparator);
	private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<>(100, sellComparator);

	private final ConcurrentHashMap<Order, Order> buyOrdersProcessed = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Order, Order> sellOrdersProcessed = new ConcurrentHashMap<>();

	private final AtomicInteger buyRequests = new AtomicInteger(0);
	private final AtomicInteger sellRequests = new AtomicInteger(0);
	private final AtomicInteger buyProcessed = new AtomicInteger(0);
	private final AtomicInteger sellProcessed = new AtomicInteger(0);
	private final AtomicInteger buyCompleted = new AtomicInteger(0);
	private final AtomicInteger sellCompleted = new AtomicInteger(0);

	private final Sinks.Many<Order> buyOrderSink = Sinks.many().unicast().onBackpressureBuffer();
	private final Sinks.Many<Order> sellOrderSink = Sinks.many().unicast().onBackpressureBuffer();

	public OrderMatcher() {
		// Procesa las órdenes periódicamente
		Flux.merge(buyOrderSink.asFlux(), sellOrderSink.asFlux()).flatMap(tick -> processOrders())
				.subscribeOn(Schedulers.boundedElastic()).subscribe();
	}

	// Agregar orden de manera reactiva
	public Mono<Void> addOrder(Order order) {
		if (order.orderType() == OrderType.BUY) {
			return Mono.fromRunnable(() -> {
				buyOrders.add(order);
				buyOrderSink.tryEmitNext(order); // Emitir la nueva orden de compra
			});
		} else {
			return Mono.fromRunnable(() -> {
				sellOrders.add(order);
				sellOrderSink.tryEmitNext(order); // Emitir la nueva orden de venta
			});
		}
	}

	// Procesar las órdenes coincidentes
	private Mono<Void> processOrders() {
		return Mono.fromRunnable(() -> {
			if (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
				Order bo = buyOrders.poll(), so = sellOrders.poll();

				if (!sellOrdersProcessed.containsKey(so) && !buyOrdersProcessed.containsKey(bo)
						&& bo.price() >= so.price()) {
					sellOrdersProcessed.putIfAbsent(so, bo);
					buyOrdersProcessed.putIfAbsent(bo, so);

					buyProcessed.incrementAndGet();
					sellProcessed.incrementAndGet();
				} else {
					addOrder(so);
					addOrder(bo);
				}
			}
		});
	}

	// Verifica si una orden ha sido procesada y devuelve el par correspondiente
	public Mono<Order> matchOrder(Order currentOrder) {
		if (currentOrder.orderType() == OrderType.BUY) {
			buyRequests.incrementAndGet();
			return Mono.defer(() -> Mono.justOrEmpty(buyOrdersProcessed.remove(currentOrder))).doOnSuccess(order -> {
				if (Objects.nonNull(order)) {
					buyCompleted.incrementAndGet();
				}
			}).repeatWhenEmpty(repeatSignal -> repeatSignal.delayElements(Duration.ofMillis(SLEEPING_TIME)))
					.timeout(Duration.ofSeconds(TIMEOUT))
					.doOnError(TimeoutException.class, ex -> buyOrders.remove(currentOrder));
		} else {
			sellRequests.incrementAndGet();
			return Mono.defer(() -> Mono.justOrEmpty(sellOrdersProcessed.remove(currentOrder))).doOnSuccess(order -> {
				if (Objects.nonNull(order)) {
					sellCompleted.incrementAndGet();
				}
			}).repeatWhenEmpty(repeatSignal -> repeatSignal.delayElements(Duration.ofMillis(SLEEPING_TIME)))
					.timeout(Duration.ofSeconds(TIMEOUT))
					.doOnError(TimeoutException.class, ex -> sellOrders.remove(currentOrder));
		}
	}

	// Devuelve el estado actual de todas las órdenes
	public Mono<ApiResponse> getAllOrders() {
		return Mono.just(new ApiResponse(true, "All orders in process",
				new AvailableOrdersOrdersInProcess(buyOrders.stream().toList(), sellOrders.stream().toList())));
	}

	// Devuelve las estadísticas de las órdenes procesadas
	public Mono<OrderStats> getStatistics() {
		return Mono.just(new OrderStats(buyRequests.get(), sellRequests.get(), buyProcessed.get(), sellProcessed.get(),
				buyCompleted.get(), sellCompleted.get(), buyOrdersProcessed.size(), sellOrdersProcessed.size()));
	}
}
