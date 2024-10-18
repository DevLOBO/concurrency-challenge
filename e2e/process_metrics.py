from requests import get

memory_used = get('http://localhost:8080/actuator/metrics/jvm.memory.used').json()['measurements'][0]['value'] / 1048576
cpu_usage = get('http://localhost:8080/actuator/metrics/system.cpu.usage').json()['measurements'][0]['value'] * 100

print(f"Uso de CPU: {cpu_usage:.2f}%")
print(f"Memoria RAM usada: {memory_used:,.2f}MB")