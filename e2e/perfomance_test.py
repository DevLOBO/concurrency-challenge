from locust import HttpUser, TaskSet, task, between
from random import choice

class Tasks(TaskSet):
	@task(1)
	def send_buy_order(self):
		url = f"/orders/submit?quantity=1&price=100&ordertype=BUY"
		self.client.post(url)
	
	@task(1)
	def send_sell_order(self):
		url = f"/orders/submit?ordertype=SELL&quantity=1&price=100"
		self.client.post(url)

class ApiUser(HttpUser):
	tasks = [Tasks]
	wait_time = between(0.5, 1)