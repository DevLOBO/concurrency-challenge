from locust import HttpUser, TaskSet, task, between

class Tasks(TaskSet):
	@task(1)
	def send_buy_order(self):
		url_buy = f"/orders/submit?quantity=1&price=100&ordertype=BUY"
		url_sell = f"/orders/submit?quantity=1&price=100&ordertype=SELL"
		self.client.post(url_buy)
		self.client.post(url_sell)
	
	@task(1)
	def send_sell_order(self):
		url_buy = f"/orders/submit?quantity=1&price=100&ordertype=BUY"
		url_sell = f"/orders/submit?quantity=1&price=100&ordertype=SELL"
		self.client.post(url_sell)
		self.client.post(url_buy)

class ApiUser(HttpUser):
	tasks = [Tasks]
	wait_time = between(0.5, 1)