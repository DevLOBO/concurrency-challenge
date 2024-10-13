package coe.unosquare;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import coe.unosquare.model.ApiResponse;
import coe.unosquare.model.AvailableOrdersOrdersInProcess;

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "30s")
class IntegrationTests {
	private static final String ORDERS_VIEW_URL = "/orders/view";
	private static final String ORDERS_SUBMIT_URL = "/orders/submit?quantity=1&price=%d&ordertype=%s";

	private ExecutorService executor;

	@Autowired
	private WebTestClient webClient;

	@BeforeEach
	public void setupExecutor() {
		executor = Executors.newFixedThreadPool(8);
	}

	@AfterEach
	public void shutdownExecutor() {
		executor.shutdown();
	}

	@Test
	void testIfAOrderisAddedIntoQueue() {
		CompletableFuture<Void> timeoutRequest = CompletableFuture.runAsync(() -> webClient.post()
				.uri(String.format(ORDERS_SUBMIT_URL, 100, "BUY")).exchange().expectStatus().isBadRequest(), executor);
		CompletableFuture<Void> validateOrdersRequest = CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			webClient.get().uri(ORDERS_VIEW_URL).exchange().expectStatus().isOk().expectBody(ApiResponse.class)
					.value(response -> {
						AvailableOrdersOrdersInProcess data = (AvailableOrdersOrdersInProcess) response.data();

						assertThat(data.buyOrders()).isNotNull().hasSize(1);
						assertThat(data.sellOrders()).isNotNull().isEmpty();
					});
		}, executor);

		CompletableFuture.allOf(timeoutRequest, validateOrdersRequest);
	}

	@Test
	void testIfTwoOrdersAreProcessed() {
		CompletableFuture<Void> sendBuyOrder = CompletableFuture.runAsync(() -> webClient.post()
				.uri(String.format(ORDERS_SUBMIT_URL, 100, "BUY")).exchange().expectStatus().isOk(), executor);
		CompletableFuture<Void> sendSellOrder = CompletableFuture.runAsync(() -> webClient.post()
				.uri(String.format(ORDERS_SUBMIT_URL, 100, "SELL")).exchange().expectStatus().isOk(), executor);

		CompletableFuture.allOf(sendBuyOrder, sendSellOrder);
	}
}
