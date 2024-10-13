package coe.unosquare.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import lombok.extern.slf4j.Slf4j;

public class OrderMatcher {
	private static final int TIMEOUT_DURATION = 10000;
	private static final int SLEEPING_TIME = 50;

	private static final Comparator<Order> buyComparator = Comparator
			.comparing((Order order) -> -1 * order.getPrice().get())
			.thenComparing((Order order) -> order.getTimestamp().get());
	private static final Comparator<Order> sellComparator = Comparator
			.comparing((Order order) -> order.getPrice().get())
			.thenComparing((Order order) -> order.getTimestamp().get());

	private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<Order>(100, buyComparator);
	private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<Order>(100, sellComparator);

	private static final ConcurrentHashMap<Order, Order> buyOrdersProcessed = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Order, Order> sellOrdersProcessed = new ConcurrentHashMap<>();

	public synchronized ApiResponse getAllOrders() {
		return new ApiResponse(true, "All orders in process", new AvailableOrdersOrdersInProcess(
				Arrays.asList(buyOrders.toArray(new Order[0])), Arrays.asList(sellOrders.toArray(new Order[0]))));
	}

	// Validate the order type and add into the corresponding queue.
	public synchronized void addOrder(Order order) {
		if (order.getType().get().compareTo(Order.OrderType.BUY) == 0)
			buyOrders.add(order);
		else
			sellOrders.add(order);
	}

	// Use Optional to wrap the next buy order (or empty if none).
	public Optional<Order> getNextBuyOrder() {
		synchronized (buyOrders) {
			return Optional.ofNullable(buyOrders.peek());
		}
	}

	// Use Optional to wrap the next sell order (or empty if none).
	public Optional<Order> getNextSellOrder() {
		synchronized (sellOrders) {
			return Optional.ofNullable(sellOrders.peek());
		}
	}

	/*
	 * checks for pairs of buy and sell orders with compatible prices (where the buy
	 * order price is greater than or equal to the sell order price) and removes
	 * them from the queues once matched, simulating a trade between those orders.
	 */
	public void matchOrders(Order currentOrder) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while (true) {
			synchronized (this) {
				long duration = System.currentTimeMillis() - startTime;
				if (duration >= TIMEOUT_DURATION && currentOrder.getType().get() == Order.OrderType.BUY) {
					buyOrders.remove(currentOrder);
					throw new OrderTimeoutException();
				} else if (duration >= TIMEOUT_DURATION && currentOrder.getType().get() == Order.OrderType.SELL) {
					sellOrders.remove(currentOrder);
					throw new OrderTimeoutException();
				}

				if (currentOrder.getType().get() == Order.OrderType.BUY
						&& buyOrdersProcessed.containsKey(currentOrder)) {
					Order sellOrder = buyOrdersProcessed.get(currentOrder);
					buyOrdersProcessed.remove(currentOrder, sellOrder);
					sellOrdersProcessed.remove(sellOrder, currentOrder);
					return;
				} else if (currentOrder.getType().get() == Order.OrderType.SELL
						&& sellOrdersProcessed.containsKey(currentOrder)) {
					Order buyOrder = sellOrdersProcessed.get(currentOrder);
					buyOrdersProcessed.remove(buyOrder, currentOrder);
					sellOrdersProcessed.remove(currentOrder, buyOrder);
					return;
				}

				if (!(buyOrders.isEmpty() || sellOrders.isEmpty())) {
					double buyPrice = getNextBuyOrder().get().getPrice().get();
					double sellPrice = getNextSellOrder().get().getPrice().get();

					if (buyPrice >= sellPrice) {
						Order buyOrderProcessed = buyOrders.poll(), sellOrderProcessed = sellOrders.poll();

						buyOrdersProcessed.putIfAbsent(buyOrderProcessed, sellOrderProcessed);
						sellOrdersProcessed.putIfAbsent(sellOrderProcessed, buyOrderProcessed);
						return;
					}
				}
			}

			Thread.sleep(SLEEPING_TIME);
		}
	}
}
