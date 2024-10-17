from concurrent.futures import ThreadPoolExecutor, as_completed
from requests import post, get
from json import dump
from time import sleep, time

url = 'http://localhost:8080/orders'
prices = {i * 100 for i in range(1, 11)}

def view_orders(sleep_time):
	sleep(sleep_time)
	response = get(f"{url}/view").json()
	print(response)

def send_request(sleep_time, type, price):
	sleep(sleep_time)
	params = {'price': price, 'quantity': 1, 'ordertype': type}
	response = post(f"{url}/submit", params=params).json()
	return response

with ThreadPoolExecutor(max_workers=100) as executor:
	results = []
	futures = []
	input('Presiona cualquier tecla para continuar...')
	start_time = time()
	for _ in range(100000):
		futures.append(executor.submit(send_request, 0, 'BUY', 10))
		futures.append(executor.submit(send_request, 0, 'SELL', 5))
	results = [future.result() for future in as_completed(futures)]
	duration = time() - start_time
	print(f"Took {duration}")
	dump(results, open('results.json', 'w'))