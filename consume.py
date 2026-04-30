from kafka import KafkaConsumer
import json

consumer = KafkaConsumer(
    'payment_failed',
    bootstrap_servers=['localhost:9092'],
    auto_offset_reset='earliest',
    enable_auto_commit=False,
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    consumer_timeout_ms=5000
)

messages = []
for msg in consumer:
    messages.append(msg.value)

if messages:
    print("Last message:", messages[-1])
else:
    print("No messages found.")
