from json import load

order_types = {
	'BUY': 'buyOrder',
	'SELL': 'sellOrder'
}

responses = load(open('./results.json', 'r'))

groupped_data = {response['data']['currentOrder']['id']: response['data'] for response in responses}

counter = 0
data_copy = groupped_data.copy()
for id, orders in data_copy.items():
	id_matching = orders['sellOrder']['id'] if orders['currentOrder']['orderType'] == 'BUY' else orders['buyOrder']['id']
	data = groupped_data[id_matching]
	current_type = order_types[orders['currentOrder']['orderType']]
	matching_type = order_types[data['currentOrder']['orderType']]
	matching_data = groupped_data[id_matching]
	if matching_data[current_type]['id'] == id and matching_data[matching_type]['id'] == id_matching:
		counter += 1

print(counter / 2)