from requests import get, post

data = get('http://localhost:8080/orders/view').json()['data']
print(f"Ordenes de compra: {len(data['buyOrders'])}")
print(f"Ordenes de venta: {len(data['sellOrders'])}")