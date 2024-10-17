package coe.unosquare.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OrderMatcher {
	private static final int SLEEPING_TIME = 100;

	private static final Comparator<Order> buyComparator = Comparator.comparing((Order order) -> -1 * order.price())
			.thenComparing((Order order) -> order.timestamp());
	private static final Comparator<Order> sellComparator = Comparator.comparing((Order order) -> order.price())
			.thenComparing((Order order) -> order.timestamp());

	private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<Order>(100, buyComparator);
	private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<Order>(100, sellComparator);

	private static final ConcurrentHashMap<Order, Order> buyOrdersProcessed = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Order, Order> sellOrdersProcessed = new ConcurrentHashMap<>();

	private static final Lock buyLocker = new ReentrantLock(), sellLocker = new ReentrantLock();
	private static final Condition buyCondition = buyLocker.newCondition(), sellCondition = sellLocker.newCondition();

	private static final AtomicInteger buyRequests = new AtomicInteger(0), sellRequests = new AtomicInteger(0),
			buyProcessed = new AtomicInteger(0), sellProcessed = new AtomicInteger(0),
			buyCompleted = new AtomicInteger(0), sellCompleted = new AtomicInteger(0);

	public OrderMatcher() {
		new Thread(() -> {
			while (true) {
				buyLocker.lock();
				sellLocker.lock();

				boolean wasOrdersProcessed = false;
				while (!buyOrders.isEmpty() && !sellOrders.isEmpty()
						&& buyOrders.peek().price() >= sellOrders.peek().price()) {
					Order bo = buyOrders.poll(), so = sellOrders.poll();
					buyOrdersProcessed.putIfAbsent(bo, so);
					sellOrdersProcessed.putIfAbsent(so, bo);

					sellProcessed.incrementAndGet();
					buyProcessed.incrementAndGet();
					wasOrdersProcessed = true;
				}

				if (wasOrdersProcessed) {
					buyCondition.signalAll();
					sellCondition.signalAll();
				}

				buyLocker.unlock();
				sellLocker.unlock();

				try {
					Thread.sleep(SLEEPING_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	public ApiResponse getAllOrders() {
		return new ApiResponse(true, "All orders in process", new AvailableOrdersOrdersInProcess(
				Arrays.asList(buyOrders.toArray(new Order[0])), Arrays.asList(sellOrders.toArray(new Order[0]))));
	}

	// Validate the order type and add into the corresponding queue.
	public void addOrder(Order order) {
		if (order.orderType() == OrderType.BUY) {
			buyLocker.lock();
			buyOrders.add(order);
			buyLocker.unlock();
		} else {
			sellLocker.lock();
			sellOrders.add(order);
			sellLocker.unlock();
		}
	}

	/*
	 * checks for pairs of buy and sell orders with compatible prices (where the buy
	 * order price is greater than or equal to the sell order price) and removes
	 * them from the queues once matched, simulating a trade between those orders.
	 */
	public void matchOrders(Order currentOrder) throws InterruptedException {
		if (currentOrder.orderType() == OrderType.BUY) {
			buyRequests.incrementAndGet();
			buyLocker.lock();
			while (!buyOrdersProcessed.containsKey(currentOrder)) {
				buyCondition.await();
			}
			buyLocker.unlock();
		} else {
			sellRequests.incrementAndGet();
			sellLocker.lock();
			while (!sellOrdersProcessed.containsKey(currentOrder)) {
				sellCondition.await();
			}
			sellLocker.unlock();
		}

		if (currentOrder.orderType() == OrderType.BUY) {
			buyOrdersProcessed.remove(currentOrder);
			buyCompleted.incrementAndGet();
		} else {
			sellOrdersProcessed.remove(currentOrder);
			sellCompleted.incrementAndGet();
		}
	}

	public OrderStats getStatistics() {
		return new OrderStats(buyRequests.get(), sellRequests.get(), buyProcessed.get(), sellProcessed.get(),
				buyCompleted.get(), sellCompleted.get());
	}
}
