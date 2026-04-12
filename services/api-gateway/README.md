# Payment API Gateway

This is the API Gateway for the distributed payment processing architecture. 
It exposes REST endpoints for initiating payments, fetching transactions, and managing payment methods.

## Features

- RESTful API endpoints for payments.
- Idempotency check with Redis to prevent duplicate transactions.
- Asynchronous event publishing via Kafka for decoupled payment processing.

## Prerequisites

- Java 21
- Maven 3.x
- Docker & Docker Compose (for running Redis and Kafka)

## Running Locally

1. Start Redis and Kafka locally on default ports (`6379` for Redis and `9092` for Kafka).
2. Build and run the service:

```bash
mvn clean install
mvn spring-boot:run
```

The server will start on port `8080`.
